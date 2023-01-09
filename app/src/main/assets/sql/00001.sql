CREATE TABLE category (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    color_code INT NOT NULL,
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000)
);

CREATE TABLE project (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000)
);

CREATE TABLE activity_type (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000)
);

CREATE TABLE activity (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    details TEXT,
    activity_type_id INT,
    activity_category_id INT,
    project_id INT,
    starttime_at INT,
    endtime_at INT,
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),


    FOREIGN KEY(project_id) REFERENCES project(_id),
    FOREIGN KEY(activity_category_id) REFERENCES category(_id),
    FOREIGN KEY(activity_type_id) REFERENCES activity_type(_id)
);