-- Indexes for the coach read path.
--
-- t_user_property and t_user_drill_group_property are looked up by (user, key)
-- several times per request — level label, active difficulty, order index, and
-- the difficulty scoping behind the snapshot aggregates. t_user_property was
-- created outside Flyway (its CREATE TABLE is commented out in the initial
-- schema), so whatever indexes it has were added by hand and are not recorded
-- here; IF NOT EXISTS makes this safe either way.
--
-- Column order puts user_id first because every query filters on it, and some
-- filter on user_id alone (UserPropertyDao.selectByUserId, and the batched
-- findByUserIds behind the roster payload).
CREATE INDEX IF NOT EXISTS idx_user_property_user_id_key
    ON t_user_property(user_id, property_key);

CREATE INDEX IF NOT EXISTS idx_user_drill_group_property_lookup
    ON t_user_drill_group_property(user_id, drill_group_id, property_key);

-- Both the login path and AccountPendingDeletionFilter (which runs on EVERY
-- authenticated request) match on lower(username); a plain index on username
-- cannot serve that predicate, so it needs the expression indexed. Same for the
-- lower(email) lookup used by CRM/Google sign-in.
CREATE INDEX IF NOT EXISTS idx_user_lower_username
    ON t_user(lower(username));

CREATE INDEX IF NOT EXISTS idx_user_lower_email
    ON t_user(lower(email));

-- t_drill_item(order_index) supports the new single-level completion query,
-- which filters drill items by group AND level rather than loading the group.
-- Paired with the existing idx_drill_item_drill_group_id.
CREATE INDEX IF NOT EXISTS idx_drill_item_group_order_index
    ON t_drill_item(drill_group_id, order_index);
