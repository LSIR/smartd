some MySQL related commands:


to create database:
CREATE DATABASE gsn;


to create user:
CREATE USER 'gsn'@'localhost' IDENTIFIED BY 'gsnpassword';


to grant privileges:
GRANT ALL PRIVILEGES ON gsn.* TO 'gsn'@'localhost';
FLUSH PRIVILEGES;


show databases:
SHOW DATABASES;


use a database:
USE gsn;


show tables:
SHOW TABLES;


adding file *.sql into a database
1. use a database
   USE gsn;
2. import a *.sql file: 
   source /path/to/sql/file.sql


rename tables:
RENAME TABLE old_table_name TO new_table_name;


