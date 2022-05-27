ALTER TABLE spool DROP FOREIGN KEY spool_ibfk_1;

ALTER TABLE mail_user DROP FOREIGN KEY mail_user_ibfk_1;

ALTER TABLE domain RENAME TO mail_domain;

ALTER TABLE mail_domain CHANGE COLUMN domain mail_domain varchar(200) not null;

ALTER TABLE mail_domain ADD COLUMN guessPermission TINYINT default 0;

ALTER TABLE mail_user CHANGE COLUMN domain mail_domain varchar(200) not null;

ALTER TABLE mail_user CHANGE COLUMN passwd passwd varchar(41);

ALTER TABLE spool CHANGE COLUMN next next_send datetime;

ALTER TABLE spool CHANGE COLUMN domain mail_domain varchar(200) not null;

ALTER TABLE imap_data CHANGE COLUMN size filesize int(11) not null;

CREATE TABLE dbproperty (
  id INT NOT NULL AUTO_INCREMENT,
  propname VARCHAR(64) NOT NULL,
  propval VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE IDX_dbproperty_1 (propname)
);

ALTER TABLE mail_user ADD FOREIGN KEY (mail_domain) REFERENCES mail_domain (mail_domain);

ALTER TABLE mail_user ADD COLUMN quota int(11) default 0;

ALTER TABLE Response RENAME TO response;

ALTER TABLE response DROP FOREIGN KEY response_ibfk_1;

ALTER TABLE response CHANGE COLUMN id_mail_user mail_user_id int(11) not null;

ALTER TABLE response ADD FOREIGN KEY (mail_user_id) REFERENCES mail_user (id);

ALTER TABLE forward DROP FOREIGN KEY forward_ibfk_1;

ALTER TABLE forward CHANGE COLUMN user_id mail_user_id int(11) not null;

ALTER TABLE forward ADD FOREIGN KEY (mail_user_id) REFERENCES mail_user (id);

INSERT INTO dbproperty (propname, propval) values ('version', '1.4');
