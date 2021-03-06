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
l_body INT
);

CREATE INDEX IDX_imap_mime_1 on imap_mime(filename,path);


CREATE TABLE mail_domain(
mail_domain VARCHAR(200) NOT NULL PRIMARY KEY,
owner VARCHAR(100) NOT NULL
);


CREATE TABLE spool(
id INT GENERATED BY DEFAULT AS IDENTITY,
filename CHAR(64) NOT NULL,
name VARCHAR(100) NOT NULL,
mail_domain VARCHAR(200) NOT NULL,
next_send DATETIME,
retry INT NOT NULL,
s_name VARCHAR(100) NOT NULL,
s_domain VARCHAR(200) NOT NULL,
CONSTRAINT fk_spool1 FOREIGN KEY (s_domain) REFERENCES mail_domain (mail_domain)
);

CREATE INDEX IDX_spool_1 on spool("next");


CREATE TABLE mail_user(
id INT GENERATED BY DEFAULT AS IDENTITY,
name VARCHAR(100) NOT NULL,
mail_domain VARCHAR(200) NOT NULL,
passwd CHAR(30) NOT NULL,
last DATETIME,
address CHAR(15),
keep TINYINT DEFAULT 1,
reply TINYINT DEFAULT 0,
CONSTRAINT fk_mu_1 FOREIGN KEY (mail_domain) REFERENCES mail_domain (mail_domain)
);

CREATE UNIQUE INDEX IDX_mail_user_1 on mail_user (name,mail_domain);


CREATE TABLE forward(
id INT GENERATED BY DEFAULT AS IDENTITY,
user_id INT NOT NULL,
forward VARCHAR(255) NOT NULL,
notify TINYINT DEFAULT 0,
CONSTRAINT fk_fw_1 FOREIGN KEY (user_id) REFERENCES mail_user (id)
);

CREATE INDEX IDX_forward_1 on forward(user_id,id);


CREATE TABLE imap_unit(
id INT GENERATED BY DEFAULT AS IDENTITY,
base VARCHAR(128)
);


CREATE TABLE imap_folder(
id INT GENERATED BY DEFAULT AS IDENTITY,
imap_unit_id INT NOT NULL,
path VARCHAR(255) NOT NULL,
last DATETIME,
CONSTRAINT fk_if_1 FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id)
);

CREATE INDEX IDX_imap_folder_1 on imap_folder (imap_unit_id,path);


CREATE TABLE mail_user_imap_unit(
mail_user_id INT NOT NULL,
imap_unit_id INT NOT NULL,
r_write TINYINT,
r_delete TINYINT,
CONSTRAINT fk_muiu_1 FOREIGN KEY (imap_unit_id) REFERENCES imap_unit (id),
CONSTRAINT fk_muiu_2 FOREIGN KEY (mail_user_id) REFERENCES mail_user (id)
);

CREATE UNIQUE INDEX IDX_muiu_1 on mail_user_imap_unit (mail_user_id,imap_unit_id);


CREATE TABLE imap_subs(
id INT GENERATED BY DEFAULT AS IDENTITY,
mail_user_id INT NOT NULL,
path VARCHAR(255) NOT NULL,
CONSTRAINT fk_is_1 FOREIGN KEY (mail_user_id) REFERENCES mail_user (id)
);


CREATE TABLE imap_data(
id INT GENERATED BY DEFAULT AS IDENTITY,
imap_folder_id INT NOT NULL,
imap_mime_filename CHAR(64),
last DATETIME,
f_answered TINYINT,
f_flagged TINYINT,
f_deleted TINYINT,
f_seen TINYINT,
f_draft TINYINT,
filesize INT NOT NULL,
CONSTRAINT fk_id_1 FOREIGN KEY (imap_folder_id) REFERENCES imap_folder (id)
);

CREATE INDEX IDX_id_1 on imap_data (imap_mime_filename);

CREATE TABLE response(
id INT GENERATED BY DEFAULT AS IDENTITY,
id_mail_user INT NOT NULL,
response VARCHAR(255),
CONSTRAINT fk_response_1 FOREIGN KEY (id_mail_user) REFERENCES mail_user (id)
);
