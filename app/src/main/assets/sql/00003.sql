ALTER TABLE category ADD COLUMN modified_at INT;
ALTER TABLE project ADD COLUMN modified_at INT;
ALTER TABLE project ADD COLUMN color_code INT;
UPDATE category SET modified_at = created_at;
UPDATE project SET modified_at = created_at;
