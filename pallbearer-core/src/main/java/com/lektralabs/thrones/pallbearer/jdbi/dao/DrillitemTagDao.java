package com.lektralabs.thrones.pallbearer.jdbi.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.util.UUID;

public interface DrillItemTagDao {

    @SqlUpdate("INSERT INTO t_drill_item_tag (drill_item_id, tag_id) VALUES (:drillItemId, :tagId) ON CONFLICT DO NOTHING")
    int insertTag(@Bind("drillItemId") UUID drillItemId, @Bind("tagId") UUID tagId);

    @SqlUpdate("DELETE FROM t_drill_item_tag WHERE drill_item_id = :drillItemId AND tag_id = :tagId")
    int deleteTag(@Bind("drillItemId") UUID drillItemId, @Bind("tagId") UUID tagId);

    @SqlUpdate("DELETE FROM t_drill_item_tag WHERE drill_item_id = :drillItemId")
    int deleteAllForDrillItem(@Bind("drillItemId") UUID drillItemId);
}
