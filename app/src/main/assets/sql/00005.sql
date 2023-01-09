CREATE TABLE custom_field_type (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000)
);

CREATE TABLE custom_field_value (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    field_value INT NOT NULL,
    custom_field_type_id INT NOT NULL,
    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),

    FOREIGN KEY(custom_field_type_id) REFERENCES custom_field_type(_id)
);

CREATE TABLE activity_custom_field_value (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    custom_field_value_id INT NOT NULL,
    activity_id INT NOT NULL,

    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),

    FOREIGN KEY(custom_field_value_id) REFERENCES custom_field_value(_id),
    FOREIGN KEY(activity_id) REFERENCES activity(_id)
);
