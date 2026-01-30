-- Remove the foreign key constraint first (if it exists)
ALTER TABLE t_drill_item_import 
    DROP CONSTRAINT IF EXISTS t_drill_item_import_media_id_fkey;

-- Change media_id from UUID to VARCHAR to accept string values (URLs, paths, etc.)
-- Using USING clause to convert existing UUID values to text
ALTER TABLE t_drill_item_import 
    ALTER COLUMN media_id TYPE VARCHAR(2048) USING media_id::text;
