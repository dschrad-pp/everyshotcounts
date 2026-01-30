# Drill Item Import Feature

This feature allows you to import drill item data from a CSV file into a replica table `t_drill_item_import`.

## Database Table

A new table `t_drill_item_import` has been created as a replica of `t_drill_item` table. The migration file is located at:
- `pallbearer-api/src/main/resources/db/migration/V202512231116__drill_item_import_table.sql`

## CSV Format

The CSV file should have the following columns (in order):

1. **drill_group_id** (UUID, required) - The UUID of the drill group
2. **name** (String, optional) - Name of the drill item
3. **description** (String, optional) - Description of the drill item
4. **media_id** (String, optional) - Media ID reference. Can be:
   - A URL (e.g., `https://drive.google.com/file/d/1nxiNvs8JToHaXzTJto4jk8m3-tgKYdno/view?usp=sharing`)
   - A file path (e.g., `/path/to/media/video.mp4`)
   - A UUID string (e.g., `550e8400-e29b-41d4-a716-446655440000`)
   - Any other string identifier
   - The database field accepts VARCHAR(2048) to store any string value
5. **media_thumbnail** (String, optional) - Media thumbnail URL/path
6. **level_index** (Integer, optional, default: 1) - Level index
7. **drill_item_order** (Integer, optional, default: 1) - Order within the drill
8. **order_index** (Integer, optional) - Order index
9. **passing_score** (Integer, optional, default: 3) - Minimum passing score
10. **visibility_code** (String, optional, default: "PRIVATE") - Visibility code (e.g., "PUBLIC", "PRIVATE", "PROTECTED")
11. **allow_retry_code** (String, optional, default: "ACTIVE") - Retry code (e.g., "ACTIVE", "INACTIVE")
12. **retry_max** (Integer, optional, default: 3) - Maximum number of retries
13. **time_limit_ms** (Long, optional, default: 300000) - Time limit in milliseconds
14. **level_test** (Boolean, optional, default: false) - Whether this is a level test
15. **team_id** (UUID, optional) - Team ID reference
16. **shots_max** (Integer, optional, default: 20) - Maximum number of shots

### Sample CSV

See `sample_drill_item_import.csv` for a sample CSV file with example data.

## API Endpoints

### Import CSV File

**POST** `/api/drill_item_import/import`

Upload a CSV file to import drill items.

**Request:**
- Method: POST
- Content-Type: multipart/form-data
- Form field: `file` (the CSV file)
- Required roles: ADMIN, COACH

**Response:**
```json
{
  "message": "Successfully imported 5 records",
  "count": 5
}
```

**Example using curl:**
```bash
curl -X POST \
  http://localhost:8080/api/drill_item_import/import \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@sample_drill_item_import.csv"
```

### Get All Imported Items

**GET** `/api/drill_item_import/`

Retrieve all imported drill items.

**Response:**
```json
[
  {
    "id": "...",
    "drillGroupId": "...",
    "name": "...",
    ...
  }
]
```

### Get Single Imported Item

**GET** `/api/drill_item_import/{id}`

Retrieve a specific imported drill item by ID.

### Delete All Imported Items

**DELETE** `/api/drill_item_import/`

Delete all records from the import table.

**Required roles:** ADMIN

**Response:**
```json
{
  "message": "Deleted 5 records",
  "count": 5
}
```

## Implementation Details

### Files Created

1. **Model:** `pallbearer-core/src/main/java/com/lektralabs/thrones/pallbearer/jdbi/model/generated/DrillItemImportRow.java`
2. **DAO:** `pallbearer-core/src/main/java/com/lektralabs/thrones/pallbearer/jdbi/dao/DrillItemImportDao.java`
3. **DAO SQL:** `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/DrillItemImportDao.sql.stg`
4. **Service:** `pallbearer-core/src/main/java/com/lektralabs/thrones/pallbearer/jdbi/service/DrillItemImportService.java`
5. **Resource:** `pallbearer-api/src/main/java/com/lektralabs/thrones/pallbearer/api/resource/DrillItemImportResource.java`
6. **Migration:** `pallbearer-api/src/main/resources/db/migration/V202512231116__drill_item_import_table.sql`

### CSV Parsing

The CSV parser:
- Handles quoted values (values containing commas)
- Skips empty lines
- Provides default values for optional fields
- Validates required fields (drill_group_id)
- Continues processing even if individual lines fail
- Logs warnings for problematic lines

### Error Handling

- Invalid UUIDs are rejected with clear error messages
- Missing required fields cause the line to be skipped
- The import continues even if some lines fail
- All errors are logged for debugging

## Notes

- The import is transactional - if any error occurs, the entire import is rolled back
- All imported records get automatic timestamps and audit fields
- The `id` field is auto-generated for each imported record
- Empty or null values in optional fields use the default values specified above

