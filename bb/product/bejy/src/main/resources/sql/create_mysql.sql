-- version: 1.4
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
l_body INT,
INDEX IDX_imap_mime_1 (filename,path));


CREATE TABLE mail_domain(
mail_domain VARCHAR(200) NOT NULL,
owner VARCHAR(100) NOT NULL,
guessPermission TINYINT default 0,
PRIMARY KEY (mail_domain));


CREATE TABLE spool(
id INT NOT NULL AUTO_INCREMENT,
filename CHAR(64) NOT NULL,
name VARCHAR(100) NOT NULL,
mail_domain VARCHAR(200) NOT NULL,
next_send DATETIME,
retry INT NOT NULL,
s_name VARCHAR(100) NOT NULL,
s_domain VARCHAR(200) NOT NULL,
FOREIGN KEY (s_domain) REFERENCES mail_domain (mail_domain),
PRIMARY KEY (id),
INDEX IDX_spool_1 (next_send));


CREATE TABLE mail_user(
id INT NOT NULL AUTO_INCREMENT,
name VARCHAR(100) NOT NULL,
mail_domain VARCHAR(200) NOT NULL,
passwd CHAR(30) NOT NULL,
last DATETIME,
address CHAR(15),
keep TINYINT DEFAULT 1,
reply TINYINT DEFAULT 0,
quota INT(11) DEFAULT 0,
FOREIGN KEY (mail_domain) REFERENCES mail_domain (mail_domain),
PRIMARY KEY (id),
UNIQUE IDX_mail_user_1 (name,mail_domain));


CREATE TABLE forward(
id INT NOT NULL AUTO_INCREMENT,
user_id INT NOT NULL,
forward VARCHAR(255) NOT NULL,
notify TINYINT DEFAULT 0,
FOREIGN KEY (user_id) REFERENCES mail_user (id),
PRIMARY KEY (id),
INDEX IDX_forward_1 (user_id,id));


CREATE TABLE imap_unit(
id INT NOT NULL AUTO_INCREMENT,
base VARCHAR(128),
PRIMARY KEY (id));


CREATE TABLE imap_folder(
id INT NOT NULL AUTO_INCREMENT,
imap_unit_id INT NOT NULL,
path VARCHAR(255) NOT NULL,
last DATETIME,
FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id),
PRIMARY KEY (id),
INDEX IDX_imap_folder_1 (imap_unit_id,path));


CREATE TABLE mail_user_imap_unit(
mail_user_id INT NOT NULL,
imap_unit_id INT NOT NULL,
r_write TINYINT,
r_delete TINYINT,
FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id),
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id),
PRIMARY KEY (mail_user_id,imap_unit_id));


CREATE TABLE imap_subs(
id INT NOT NULL AUTO_INCREMENT,
mail_user_id INT NOT NULL,
path VARCHAR(255) NOT NULL,
FOREIGN KEY (mail_user_id) REFERENCES mail_user (id),
PRIMARY KEY (id));


CREATE TABLE imap_data(
id INT NOT NULL AUTO_INCREMENT,
imap_folder_id INT NOT NULL,
imap_mime_filename CHAR(64),
last DATETIME,
f_answered TINYINT,
f_flagged TINYINT,
f_deleted TINYINT,
f_seen TINYINT,
f_draft TINYINT,
size INT NOT NULL,
FOREIGN KEY (imap_folder_id) REFERENCES imap_folder (id),
PRIMARY KEY (id),
INDEX IDX_imap_data_1 (imap_mime_filename));


CREATE TABLE response(
id INT NOT NULL AUTO_INCREMENT,
id_mail_user INT NOT NULL,
response VARCHAR(255),
FOREIGN KEY (id_mail_user) REFERENCES mail_user (id),
PRIMARY KEY (id),
UNIQUE UC_id (id));

CREATE TABLE dbproperty (
  id INT NOT NULL AUTO_INCREMENT,
  propname VARCHAR(64) NOT NULL,
  propval VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE IDX_dbproperty_1 (propname)
);
