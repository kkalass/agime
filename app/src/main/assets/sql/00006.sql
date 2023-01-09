ALTER TABLE custom_field_type ADD COLUMN any_project INT;

CREATE TABLE project_custom_field_type (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    project_id INT NOT NULL,
    custom_field_type_id INT NOT NULL,

    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),

    FOREIGN KEY(custom_field_type_id) REFERENCES custom_field_type(_id),
    FOREIGN KEY(project_id) REFERENCES project(_id)
);
