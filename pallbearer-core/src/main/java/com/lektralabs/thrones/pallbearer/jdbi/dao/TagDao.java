package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TagCategoryRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TagRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import java.util.List;
import java.util.UUID;

public interface TagDao {

    @RegisterBeanMapper(TagCategoryRow.class)
    @SqlQuery("SELECT id, code, name, display_order FROM t_tag_category ORDER BY display_order")
    List<TagCategoryRow> findAllTagCategories();

    @RegisterBeanMapper(TagRow.class)
    @SqlQuery("SELECT t.id, t.tag_category_id, t.code, t.name, t.display_order " +
              "FROM t_tag t JOIN t_drill_item_tag dit ON dit.tag_id = t.id " +
              "WHERE dit.drill_item_id = :drillItemId ORDER BY t.display_order")
    List<TagRow> findByDrillItemId(@Bind("drillItemId") UUID drillItemId);

    @RegisterBeanMapper(TagRow.class)
    @SqlQuery("SELECT id, tag_category_id, code, name, display_order FROM t_tag WHERE code IN (<codes>)")
    List<TagRow> findByCodes(@BindList("codes") List<String> codes);

    @RegisterBeanMapper(TagRow.class)
    @SqlQuery("SELECT id, tag_category_id, code, name, display_order FROM t_tag ORDER BY display_order")
    List<TagRow> findAllTags();

    @RegisterBeanMapper(TagRow.class)
    @SqlQuery("SELECT dit.drill_item_id, t.id, t.tag_category_id, t.code, t.name, t.display_order " +
          "FROM t_tag t JOIN t_drill_item_tag dit ON dit.tag_id = t.id " +
          "WHERE dit.drill_item_id IN (<drillItemIds>) ORDER BY t.display_order")
    List<TagRow> findByDrillItemIds(@BindList("drillItemIds") List<UUID> drillItemIds);
}
