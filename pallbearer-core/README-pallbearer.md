README-pallbearer
=================

# Class Heirarchy

Resource, Service, Dao, stg file

# Data Modeling

## Data Modeling Packages

* `detail`  - composite entities, surfaced to the UI / output
* `item` - A subset of entity data intended for use in detail models
* `generated` - 1-1 object from the DB, created by Kreator
* `partial`  - represents minimal input to create/update item / input
* `keycloak` - used to communicate with Keycloak server

## Data Naming Conventions

* `Row` - represents something in the DB in 1-1
* `Detail`  - represent a hierarchy
* `Item`    - represent an item in a hierarchy
* `Partial` - represents minimal input to create/update item

## Data Cleanup

### Cleaning up accidental userRow creation

```sql
WITH cte_user_ids AS (
  SELECT id FROM t_user WHERE username = 'carol'
), 
cte_delete_one AS (
  DELETE FROM t_user_role_xref WHERE user_id IN (SELECT id FROM cte_user_ids)
), 
DELETE FROM t_user WHERE id IN (SELECT id FROM cte_user_ids);
```

----
