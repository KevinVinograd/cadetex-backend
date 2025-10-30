-- Add courier_notes column to tasks table
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS courier_notes TEXT;




