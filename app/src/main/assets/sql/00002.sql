ALTER TABLE activity_type RENAME TO activity_type_delete;
CREATE TABLE activity_type (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    activity_category_id INT,
    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),

    FOREIGN KEY(activity_category_id) REFERENCES category(_id)
);

ALTER TABLE activity RENAME TO activity_delete;
CREATE TABLE activity (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    details TEXT,
    activity_type_id INT,
    project_id INT,
    starttime_at INT,
    endtime_at INT,
    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),

    FOREIGN KEY(project_id) REFERENCES project(_id),
    FOREIGN KEY(activity_type_id) REFERENCES activity_type(_id)
);


INSERT INTO activity_type (_id, name, activity_category_id, created_at, modified_at)
   SELECT t._id, t.name, max(a.activity_category_id), t.created_at, t.created_at
   FROM activity_type_delete t LEFT OUTER JOIN activity_delete a ON t._id = a.activity_type_id
   GROUP BY t._id ;

INSERT INTO activity (_id, details, activity_type_id, project_id, starttime_at, endtime_at, modified_at, created_at)
   SELECT a._id, a.details, a.activity_type_id, a.project_id, a.starttime_at, a.endtime_at, a.created_at, a.created_at
   FROM activity_delete a;

DROP TABLE activity_delete;
DROP TABLE activity_type_delete;