some mysql related commands:


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
2. execute: 
   mysql> source /path/to/sql/file.sql

