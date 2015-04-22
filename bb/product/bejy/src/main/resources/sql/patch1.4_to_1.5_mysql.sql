create index IDX_imap_folder_2 on imap_folder (imap_unit_id);
create index IDX_imap_data_2 on imap_data (imap_folder_id);
create index IDX_imap_data_3 on imap_data (last);
create index IDX_imap_data_4 on imap_data (imap_mime_filename);

update dbproperty set propval = '1.5' where propname = 'version';
