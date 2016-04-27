-- version: 1.4
-- project-name   :BEJY mail data base
-- project-author :Stefan "Bebbo" Franke

CREATE TABLE imap_mime(
filename char(64) NOT NULL,
path varchar(255) NULL,
contentType varchar(255) NULL,
b_begin int NULL,
b_body int NULL,
b_end int NULL,
l_header int NULL,
l_body int NULL)
;

CREATE INDEX IDX_imap_mime_1 ON imap_mime (filename,path)
;

CREATE TABLE mail_domain(
mail_domain varchar(200) NOT NULL CONSTRAINT PK_domain1 PRIMARY KEY,
owner varchar(100) NOT NULL)
;


CREATE TABLE spool(
id int identity(1,1) NOT NULL CONSTRAINT PK_spool1 PRIMARY KEY,
filename char(64) NOT NULL,
name varchar(100) NOT NULL,
mail_domain varchar(200) NOT NULL,
next_send datetime NULL,
retry int NOT NULL,
s_name varchar(100) NOT NULL,
s_domain varchar(200) NOT NULL)
;

CREATE INDEX IDX_spool_1 ON spool (next)
;

CREATE TABLE mail_user(
id int identity(1,1) NOT NULL CONSTRAINT PK_mail_user1 PRIMARY KEY,
name varchar(100) NOT NULL,
mail_domain varchar(200) NOT NULL,
passwd char(30) NOT NULL,
last datetime NULL,
address char(15) NULL,
keep tinyint DEFAULT 1 NULL,
reply tinyint DEFAULT 0 NULL,
isLocked tinyint DEFAULT 0,
szLimit int DEFAULT -1,
cntLimit int DEFAULT -1
);

CREATE UNIQUE INDEX IDX_mail_user_1 ON mail_user (name,mail_domain)
;

CREATE TABLE forward(
id int identity(1,1) NOT NULL CONSTRAINT PK_forward1 PRIMARY KEY,
user_id int NOT NULL,
forward varchar(255) NOT NULL,
notify tinyint DEFAULT 0 NULL)
;

CREATE INDEX IDX_forward_1 ON forward (user_id,id)
;

CREATE TABLE imap_unit(
id int identity(1,1) NOT NULL CONSTRAINT PK_imap_unit1 PRIMARY KEY,
base varchar(128) NULL)
;


CREATE TABLE imap_folder(
id int identity(1,1) NOT NULL CONSTRAINT PK_imap_folder1 PRIMARY KEY,
imap_unit_id int NOT NULL,
path varchar(255) NOT NULL,
last datetime NULL)
;


CREATE TABLE mail_user_imap_unit(
mail_user_id int NOT NULL,
imap_unit_id int NOT NULL,
r_write tinyint NULL,
r_delete tinyint NULL,
CONSTRAINT PK_mail_user_imap_unit1 PRIMARY KEY (mail_user_id,imap_unit_id))
;


CREATE TABLE imap_subs(
id int identity(1,1) NOT NULL CONSTRAINT PK_imap_subs1 PRIMARY KEY,
mail_user_id int NOT NULL,
path varchar(255) NOT NULL)
;


CREATE TABLE imap_data(
id int identity(1,1) NOT NULL CONSTRAINT PK_imap_data1 PRIMARY KEY,
imap_folder_id int NOT NULL,
imap_mime_filename char(64) NULL,
last datetime NULL,
f_answered tinyint NULL,
f_flagged tinyint NULL,
f_deleted tinyint NULL,
f_seen tinyint NULL,
f_draft tinyint NULL,
filesize int NOT NULL)
;

CREATE INDEX IDX_imap_data_1 ON imap_data (imap_mime_filename)
;

CREATE TABLE response(
id int identity(1,1) NOT NULL CONSTRAINT PK_response1 PRIMARY KEY,
id_mail_user int NOT NULL,
response varchar(255) NULL,
CONSTRAINT UC_response1 UNIQUE(id))
;

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
ADD CONSTRAINT FK_response_1 
FOREIGN KEY (id_mail_user) REFERENCES mail_user (id)
;

