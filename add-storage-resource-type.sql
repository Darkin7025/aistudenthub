-- ============================================================
-- Migration: Add storage_resource_type column to documents table
-- Database:  PostgreSQL (Render)
-- Purpose:   Store Cloudinary resource_type (image/raw/video)
--            for proper signed URL generation and deletion
-- Run once:  psql $DATABASE_URL -f add-storage-resource-type.sql
-- ============================================================

-- Step 1: Add column if it does not already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name  = 'documents'
          AND column_name = 'storage_resource_type'
    ) THEN
        ALTER TABLE documents
            ADD COLUMN storage_resource_type VARCHAR(50) NULL;

        RAISE NOTICE 'Column storage_resource_type added successfully.';
    ELSE
        RAISE NOTICE 'Column storage_resource_type already exists — skipping.';
    END IF;
END;
$$;

-- Step 2: Back-fill PDF rows that were uploaded before this migration.
--         PDFs are stored as "raw" in Cloudinary.
UPDATE documents
SET    storage_resource_type = 'raw'
WHERE  storage_resource_type IS NULL
  AND  deleted_at IS NULL
  AND  (
           file_type = 'application/pdf'
        OR file_name ILIKE '%.pdf'
       );

-- Step 3: Everything else defaults to 'auto'
UPDATE documents
SET    storage_resource_type = 'auto'
WHERE  storage_resource_type IS NULL
  AND  deleted_at IS NULL;

-- Step 4: Verify result
SELECT
    COALESCE(storage_resource_type, '(null)') AS resource_type,
    file_type,
    COUNT(*)                                   AS total
FROM  documents
WHERE deleted_at IS NULL
GROUP BY storage_resource_type, file_type
ORDER BY storage_resource_type, file_type;
