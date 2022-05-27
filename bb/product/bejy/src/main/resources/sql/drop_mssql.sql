
ALTER TABLE response
DROP CONSTRAINT FK_response_1 
;

ALTER TABLE imap_data
DROP CONSTRAINT FK_imap_data_1 
;

ALTER TABLE imap_subs
DROP CONSTRAINT FK_imap_subs_1 
;

ALTER TABLE mail_user_imap_unit
DROP CONSTRAINT FK_mail_user_imap_unit_1 
;

ALTER TABLE mail_user_imap_unit
DROP CONSTRAINT FK_mail_user_imap_unit_2 
;

ALTER TABLE imap_folder
DROP CONSTRAINT FK_imap_folder_1 
;

ALTER TABLE forward
DROP CONSTRAINT FK_forward_1 
;

ALTER TABLE mail_user
DROP CONSTRAINT FK_mail_user_1 
;

ALTER TABLE spool
DROP CONSTRAINT FK_spool_1 
;

DROP TABLE dbproperty;

DROP TABLE response
;


DROP TABLE imap_data
;


DROP TABLE imap_subs
;


DROP TABLE mail_user_imap_unit
;


DROP TABLE imap_folder
;


DROP TABLE imap_unit
;


DROP TABLE forward
;


DROP TABLE mail_user
;


DROP TABLE spool
;


DROP TABLE mail_domain
;


DROP TABLE imap_mime
;
