League Apps Integration Testing
===============================

```bash
cd $THRONES_HOME
build-all.sh
cd pallbearer
kstart.sh
```

In another terminal window:

```bash
cd $THRONES_HOME
cd pallbearer/pallbearer-core/src/test/scripts
./league_apps_integrate.sh
<look up 6 digit code for user (TAOjapxx) you are trying to register>
<and edit league_apps_register.sh , see sql below>
./league_apps_register.sh
```

If you are getting a `INCORRECT_REGISTRATION_CODE` error, the code has changed.


```sql
 SELECT u.username
      , up.property_value
   FROM t_user u
   JOIN t_user_property up ON up.user_id = u.id
  WHERE up.property_key = 'user.registration.sixdigit.code'
```


Reset Instructions
==================

If you are getting a `USERNAME_COLLISION` error, it is time to reset.

```bash
$THRONES_HOME/pallbearer/pallbearer-core/src/main/scripts/sync-prod.sh
<clear out entry in Keycloak for users: TAOjapxx/my_test17_username, oM50NXL9>
```


Verification Instructions
=========================

You can verify your user by examining their properties, in particular their `user.drill.group`:

```sql
  SELECT u.username
       , up.property_key
       , up.property_value
    FROM t_user u
    JOIN t_user_property up ON up.user_id = u.id
   WHERE u.username = 'my_test17_username'
```
