-- Migration: Add storage_resource_type column to documents table
-- Purpose: Store Cloudinary resource_type (image/raw/video) for proper deletion and management

USE ai_study_hub;
GO

-- Add column if not exists
IF NOT EXISTS (
    SELECT * FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'dbo.documents') 
    AND name = 'storage_resource_type'
)
BEGIN
    ALTER TABLE documents
    ADD storage_resource_type VARCHAR(50) NULL;
    
    PRINT 'Column storage_resource_type added successfully';
END
ELSE
BEGIN
    PRINT 'Column storage_resource_type already exists';
END
GO

-- Update existing PDF records to use 'image' resource type
-- This assumes PDFs uploaded before this migration were stored as 'raw' type
UPDATE documents
SET storage_resource_type = 'image'
WHERE (file_type = 'application/pdf' OR file_name LIKE '%.pdf')
  AND storage_resource_type IS NULL;

PRINT CONCAT('Updated ', @@ROWCOUNT, ' PDF records to resource_type = image');
GO

-- Update other existing records to 'auto' as default
UPDATE documents
SET storage_resource_type = 'auto'
WHERE storage_resource_type IS NULL;

PRINT CONCAT('Updated ', @@ROWCOUNT, ' other records to resource_type = auto');
GO

-- Verify migration
SELECT 
    storage_resource_type,
    file_type,
    COUNT(*) as count
FROM documents
WHERE deleted_at IS NULL
GROUP BY storage_resource_type, file_type
ORDER BY storage_resource_type, file_type;
GO
