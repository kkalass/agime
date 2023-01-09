CREATE TABLE recurring_acquisition_time (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    starttime TEXT NOT NULL,
    endtime TEXT NOT NULL,
    weekdays INT NOT NULL,
    inactive_until INT,
    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000)
);

CREATE TABLE acquisition_time (
    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    starttime_at INT NOT NULL,
    endtime_at INT NOT NULL,
    recurring_id INT,
    modified_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),
    created_at INT NOT NULL DEFAULT ((julianday('now') - 2440587.5)*86400000),

    FOREIGN KEY(recurring_id) REFERENCES recurring_acquisition_time(_id)
);

