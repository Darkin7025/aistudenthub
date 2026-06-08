-- ====================================================================
-- SQL Script: Migrate VARCHAR to NVARCHAR for Vietnamese Support
-- Database: ai_study_hub
-- ====================================================================
-- Lưu ý: Bạn cần chạy đoạn script này thủ công trong SQL Server Management Studio (SSMS) 
-- hoặc DBeaver. 
-- Đoạn script này sẽ ALTER các column có chứa tiếng Việt từ VARCHAR sang NVARCHAR.

USE ai_study_hub;
GO

-- 1. Bảng folders
ALTER TABLE folders ALTER COLUMN name NVARCHAR(255) NOT NULL;
ALTER TABLE folders ALTER COLUMN description NVARCHAR(MAX) NULL;
GO

-- 2. Bảng documents
ALTER TABLE documents ALTER COLUMN title NVARCHAR(255) NOT NULL;
ALTER TABLE documents ALTER COLUMN description NVARCHAR(MAX) NULL;
ALTER TABLE documents ALTER COLUMN file_name NVARCHAR(255) NOT NULL;
ALTER TABLE documents ALTER COLUMN original_file_name NVARCHAR(255) NULL;
GO

-- 3. Bảng users
ALTER TABLE users ALTER COLUMN full_name NVARCHAR(150) NOT NULL;
GO

-- 4. Bảng chat_messages (nếu chưa phải NVARCHAR)
-- Mặc định Entity đã set là NVARCHAR(MAX), nhưng để chắc chắn:
ALTER TABLE chat_messages ALTER COLUMN message_content NVARCHAR(MAX) NOT NULL;
GO

PRINT 'Migration completed: Columns updated to NVARCHAR.';
