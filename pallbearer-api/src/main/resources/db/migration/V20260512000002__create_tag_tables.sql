CREATE TABLE t_tag_category (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(80)  NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE t_tag (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_category_id UUID        NOT NULL REFERENCES t_tag_category(id),
    code            VARCHAR(80) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    display_order   INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE t_drill_item_tag (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drill_item_id UUID NOT NULL REFERENCES t_drill_item(id) ON DELETE CASCADE,
    tag_id        UUID NOT NULL REFERENCES t_tag(id) ON DELETE CASCADE,
    UNIQUE (drill_item_id, tag_id)
);

CREATE INDEX idx_drill_item_tag_drill_item_id ON t_drill_item_tag(drill_item_id);
CREATE INDEX idx_drill_item_tag_tag_id        ON t_drill_item_tag(tag_id);
CREATE INDEX idx_tag_tag_category_id          ON t_tag(tag_category_id);
