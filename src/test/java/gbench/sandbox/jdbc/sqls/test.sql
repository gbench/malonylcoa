-- --------------------------
-- # getAllTables
-- --------------------------
show tables;

-- --------------------------
-- # getUsers
-- #cnt
-- --------------------------
select * from t_user limit ##cnt

-- --------------------------
-- # updateUserById
-- #name
-- #id
-- --------------------------
update t_user set name=#name where id=##id

-- --------------------------
-- # getTablesOf
-- #schema : 细胞名称
-- --------------------------
select * from TABLES where TABLE_SCHEMA = #schema

-- --------------------------
-- # getTablesCount2
-- #schema : 细胞名称
-- --------------------------
select count(*) from TABLES where TABLE_SCHEMA = #schema