ALTER TABLE mail_user CHANGE COLUMN passwd passwd varchar(128);

update dbproperty set propval = '1.6' where propname = 'version';
