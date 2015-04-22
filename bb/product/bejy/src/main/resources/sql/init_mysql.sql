-- version: 1.4
-- project-name   :BEJY mail data base
-- project-author :Stefan "Bebbo" Franke
-- create the database and an user

CREATE DATABASE mail;

-- MySQL 5.0 requires this:
-- comment it out for MySQL 4 or ignore the error
CREATE USER bejymail@localhost;


GRANT ALL ON mail.* to bejymail@localhost identified by 'secret'; 
FLUSH PRIVILEGES;
