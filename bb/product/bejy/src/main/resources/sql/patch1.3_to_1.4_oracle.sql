ALTER TABLE domain RENAME TO mail_domain;

ALTER TABLE mail_domain ALTER COLUMN domain RENAME TO mail_domain;

ALTER TABLE mail_user ALTER COLUMN domain RENAME TO mail_domain;

ALTER TABLE spool ALTER COLUMN next RENAME TO next_send;

ALTER TABLE imap_data ALTER COLUMN "size" RENAME TO filesize;

CREATE TABLE dbproperty (
id INTEGER NOT NULL CONSTRAINT PK_dbproperty1 PRIMARY KEY,
propname VARCHAR2(64) NOT NULL,
propval VARCHAR2(128) NOT NULL,
CONSTRAINT UC_dbproperty1 UNIQUE(id)
);

CREATE SEQUENCE dbproperty_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_dbproperty_id
BEFORE INSERT
ON dbproperty
FOR EACH ROW
BEGIN
  SELECT dbproperty_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/

CREATE UNIQUE INDEX IDX_dbproperty_1 ON dbproperty (dbproperty);
