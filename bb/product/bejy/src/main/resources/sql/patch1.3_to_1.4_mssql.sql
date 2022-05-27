ALTER TABLE domain RENAME TO mail_domain;

ALTER TABLE mail_domain ALTER COLUMN domain RENAME TO mail_domain;

ALTER TABLE mail_user ALTER COLUMN domain RENAME TO mail_domain;

ALTER TABLE spool ALTER COLUMN next RENAME TO next_send;

ALTER TABLE imap_data ALTER COLUMN size RENAME TO filesize;

CREATE TABLE dbproperty (
  id int identity(1,1) NOT NULL CONSTRAINT PK_dbproperty1 PRIMARY KEY,  
  propname varchar(64) NOT NULL UNIQUE,
  propval varchar(128) NOT NULL
);

CREATE UNIQUE INDEX IDX_dbproperties ON dbproperties (propname);

ALTER TABLE mail_user ADD COLUMN isLocked int DEFAULT 0;

ALTER TABLE mail_user ADD COLUMN szLimit int DEFAULT -1;

ALTER TABLE mail_user ADD COLUMN cntLimit int DEFAULT -1;

