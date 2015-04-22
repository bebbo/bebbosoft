-- version: 1.3
-- project-name   :BEJY mail data base
-- project-author :Stefan "Bebbo" Franke

CREATE TABLE imap_mime(
filename CHAR(64) NOT NULL,
path VARCHAR(255),
contentType VARCHAR(255),
b_begin INT,
b_body INT,
b_end INT,
l_header INT,
l_body INT);

CREATE INDEX IDX_imap_mime_1 on imap_mime using btree (filename,path);


CREATE TABLE mail_domain(
mail_domain VARCHAR(200) NOT NULL,
owner VARCHAR(100) NOT NULL,
guessPermission smallINT default 0,
PRIMARY KEY (mail_domain));


CREATE TABLE spool(
id serial,
filename CHAR(64) NOT NULL,
name VARCHAR(100) NOT NULL,
mail_domain VARCHAR(200) NOT NULL,
next_send timestamp,
retry INT NOT NULL,
s_name VARCHAR(100) NOT NULL,
s_domain VARCHAR(200) NOT NULL,
FOREIGN KEY (s_domain) REFERENCES mail_domain (mail_domain),
PRIMARY KEY (id));

CREATE INDEX IDX_spool_1 on spool using hash (next_send);


CREATE TABLE mail_user(
id serial,
name VARCHAR(100) NOT NULL,
mail_domain VARCHAR(200) NOT NULL,
passwd CHAR(30) NOT NULL,
last timestamp,
address CHAR(15),
keep smallINT DEFAULT 1,
reply smallINT DEFAULT 0,
quota bigint DEFAULT 0,
FOREIGN KEY (mail_domain) REFERENCES mail_domain (mail_domain),
PRIMARY KEY (id),
CONSTRAINT IDX_mail_user_1 UNIQUE(name,mail_domain));


CREATE TABLE forward(
id serial,
mail_user_id INT NOT NULL,
forward VARCHAR(255) NOT NULL,
notify smallINT DEFAULT 0,
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id),
PRIMARY KEY (id));

create INDEX IDX_forward_1 on forward using btree (user_id,id);


CREATE TABLE imap_unit(
id serial,
base VARCHAR(128),
PRIMARY KEY (id));


CREATE TABLE imap_folder(
id serial,
imap_unit_id INT NOT NULL,
path VARCHAR(255) NOT NULL,
last timestamp,
FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id),
PRIMARY KEY (id));

CREATE INDEX IDX_imap_folder_1 on imap_folder using btree (imap_unit_id,path);


CREATE TABLE mail_user_imap_unit(
mail_user_id INT NOT NULL,
imap_unit_id INT NOT NULL,
r_write smallINT,
r_delete smallINT,
FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id),
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id),
PRIMARY KEY (mail_user_id,imap_unit_id));


CREATE TABLE imap_subs(
id serial,
mail_user_id INT NOT NULL,
path VARCHAR(255) NOT NULL,
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id),
PRIMARY KEY (id));


CREATE TABLE imap_data(
id serial,
imap_folder_id INT NOT NULL,
imap_mime_filename CHAR(64),
last timestamp,
f_answered smallInt,
f_flagged smallInt,
f_deleted smallInt,
f_seen smallInt,
f_draft smallInt,
filesize INT NOT NULL,
FOREIGN KEY (imap_folder_id) REFERENCES imap_folder (id),
PRIMARY KEY (id));
CREATE INDEX IDX_imap_data_1 on imap_data using btree (imap_mime_filename);


CREATE TABLE response(
id serial,
mail_user_id INT NOT NULL,
response VARCHAR(255),
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id),
PRIMARY KEY (id));

CREATE TABLE dbproperty (
  id serial,
  propname VARCHAR(64) NOT NULL,
  propval VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT IDX_dbproperty_1 UNIQUE (propname)
);

INSERT INTO dbproperty (propname, propval) values ('version', '1.3');
