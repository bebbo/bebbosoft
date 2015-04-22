-- version: 1.4
-- project-name   :BEJY mail data base
-- project-author :Stefan "Bebbo" Franke

CREATE TABLE imap_mime(
filename CHAR(64) NOT NULL,
path VARCHAR2(255),
contentType VARCHAR2(255),
b_begin INTEGER,
b_body INTEGER,
b_end INTEGER,
l_header INTEGER,
l_body INTEGER);


CREATE INDEX IDX_imap_mime_1 ON imap_mime (filename,path);

CREATE TABLE mail_domain(
mail_domain VARCHAR2(200) NOT NULL CONSTRAINT PK_domain1 PRIMARY KEY,
owner VARCHAR2(100) NOT NULL);



CREATE TABLE spool(
id INTEGER NOT NULL CONSTRAINT PK_spool1 PRIMARY KEY,
filename CHAR(64) NOT NULL,
name VARCHAR2(100) NOT NULL,
mail_domain VARCHAR2(200) NOT NULL,
next_send DATE,
retry INTEGER NOT NULL,
s_name VARCHAR2(100) NOT NULL,
s_domain VARCHAR2(200) NOT NULL);


CREATE SEQUENCE spool_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_spool_id
BEFORE INSERT
ON spool
FOR EACH ROW
BEGIN
  SELECT spool_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/

CREATE INDEX IDX_spool_1 ON spool (next);

CREATE TABLE mail_user(
id INTEGER NOT NULL CONSTRAINT PK_mail_user1 PRIMARY KEY,
name VARCHAR2(100) NOT NULL,
mail_domain VARCHAR2(200) NOT NULL,
passwd CHAR(30) NOT NULL,
last DATE,
address CHAR(15),
keep SMALLINT DEFAULT 1,
reply SMALLINT DEFAULT 0);


CREATE SEQUENCE mail_user_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_mail_user_id
BEFORE INSERT
ON mail_user
FOR EACH ROW
BEGIN
  SELECT mail_user_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/

CREATE UNIQUE INDEX IDX_mail_user_1 ON mail_user (name,mail_domain);

CREATE TABLE forward(
id INTEGER NOT NULL CONSTRAINT PK_forward1 PRIMARY KEY,
user_id INTEGER NOT NULL,
forward VARCHAR2(255) NOT NULL,
notify SMALLINT DEFAULT 0);


CREATE SEQUENCE forward_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_forward_id
BEFORE INSERT
ON forward
FOR EACH ROW
BEGIN
  SELECT forward_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/

CREATE INDEX IDX_forward_1 ON forward (user_id,id);

CREATE TABLE imap_unit(
id INTEGER NOT NULL CONSTRAINT PK_imap_unit1 PRIMARY KEY,
base VARCHAR2(128));


CREATE SEQUENCE imap_unit_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_imap_unit_id
BEFORE INSERT
ON imap_unit
FOR EACH ROW
BEGIN
  SELECT imap_unit_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/


CREATE TABLE imap_folder(
id INTEGER NOT NULL CONSTRAINT PK_imap_folder1 PRIMARY KEY,
imap_unit_id INTEGER NOT NULL,
path VARCHAR2(255) NOT NULL,
last DATE);

CREATE INDEX IDX_imap_folder_1 ON imap_folder (imap_unit_id,path);


CREATE SEQUENCE imap_folder_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_imap_folder_id
BEFORE INSERT
ON imap_folder
FOR EACH ROW
BEGIN
  SELECT imap_folder_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/


CREATE TABLE mail_user_imap_unit(
mail_user_id INTEGER NOT NULL,
imap_unit_id INTEGER NOT NULL,
r_write SMALLINT,
r_delete SMALLINT,
CONSTRAINT PK_mail_user_imap_unit1 PRIMARY KEY (mail_user_id,imap_unit_id));



CREATE TABLE imap_subs(
id INTEGER NOT NULL CONSTRAINT PK_imap_subs1 PRIMARY KEY,
mail_user_id INTEGER NOT NULL,
path VARCHAR2(255) NOT NULL);


CREATE SEQUENCE imap_subs_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_imap_subs_id
BEFORE INSERT
ON imap_subs
FOR EACH ROW
BEGIN
  SELECT imap_subs_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/


CREATE TABLE imap_data(
id INTEGER NOT NULL CONSTRAINT PK_imap_data1 PRIMARY KEY,
imap_folder_id INTEGER NOT NULL,
imap_mime_filename CHAR(64),
last DATE,
f_answered SMALLINT,
f_flagged SMALLINT,
f_deleted SMALLINT,
f_seen SMALLINT,
f_draft SMALLINT,
filesize INTEGER NOT NULL);


CREATE SEQUENCE imap_data_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_imap_data_id
BEFORE INSERT
ON imap_data
FOR EACH ROW
BEGIN
  SELECT imap_data_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/

CREATE INDEX IDX_imap_data_1 ON imap_data (imap_mime_filename);

CREATE TABLE response(
id INTEGER NOT NULL CONSTRAINT PK_response1 PRIMARY KEY,
id_mail_user INTEGER NOT NULL,
response VARCHAR2(255),
CONSTRAINT UC_response1 UNIQUE(id));


CREATE SEQUENCE response_id_SEQ
increment by 1
start with 0
NOMAXVALUE
minvalue 0
nocycle
nocache
noorder;

CREATE OR REPLACE TRIGGER SET_Response_id
BEFORE INSERT
ON response
FOR EACH ROW
BEGIN
  SELECT Response_id_SEQ.NEXTVAL
  INTO :NEW.id
  FROM DUAL;
END;
/

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



ALTER TABLE spool
ADD CONSTRAINT FK_spool_1 
FOREIGN KEY (s_domain) REFERENCES mail_domain (mail_domain)
;


ALTER TABLE mail_user
ADD CONSTRAINT FK_mail_user_1 
FOREIGN KEY (mail_domain) REFERENCES mail_domain (mail_domain)
;


ALTER TABLE forward
ADD CONSTRAINT FK_forward_1 
FOREIGN KEY (user_id) REFERENCES mail_user (id)
;



ALTER TABLE imap_folder
ADD CONSTRAINT FK_imap_folder_1 
FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id)
;


ALTER TABLE mail_user_imap_unit
ADD CONSTRAINT FK_mail_user_imap_unit_1 
FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id)
;

ALTER TABLE mail_user_imap_unit
ADD CONSTRAINT FK_mail_user_imap_unit_2 
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id)
;


ALTER TABLE imap_subs
ADD CONSTRAINT FK_imap_subs_1 
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id)
;


ALTER TABLE imap_data
ADD CONSTRAINT FK_imap_data_1 
FOREIGN KEY (imap_folder_id) REFERENCES imap_folder (id)
;


ALTER TABLE response
ADD CONSTRAINT FK_Response_1 
FOREIGN KEY (id_mail_user) REFERENCES mail_user (id)
;

