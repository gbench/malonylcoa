
-- --------------------------
-- # getAllTables
-- --------------------------
show tables;


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