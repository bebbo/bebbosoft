ALTER TABLE "domain" RENAME TO mail_domain;

ALTER TABLE mail_user ALTER COLUMN "domain" RENAME TO mail_domain;

ALTER TABLE mail_domain ALTER COLUMN "domain" RENAME TO mail_domain;

ALTER TABLE imap_data ALTER COLUMN size RENAME TO filesize;

ALTER TABLE spool ALTER COLUMN "next" RENAME TO next_send;

CREATE TABLE dbproperty (
  id INT NOT NULL AUTO_INCREMENT,
  propname VARCHAR(64) NOT NULL,
  propval VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE UC_id (id),
  UNIQUE UC_propname (propname)
);
