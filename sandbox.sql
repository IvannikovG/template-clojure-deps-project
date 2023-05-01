---- DB connection:
---- db: psql "postgresql://postgres:postgres@localhost:5435/test_chinese_dictionary_db"

----

select jsonb_pretty(resource) from chinesegrapheme
limit 1;
----
select 1;
----
select count(*) from chineserecord;
----
select jsonb_pretty(resource) from chineserecord limit 1;
----
\d
----

----
SELECT *
FROM ChineseRecord c
WHERE ( c.resource #>> '{resource, hieroglyph_id}' IN ( '1' , '3' ) )
ORDER BY c.resource #>> '{resource, hieroglyph_id}' LIMIT 1000
----
select * from app_user;
----
select c.resource
from chineserecord as c
where ((c.resource#>>'{resource,hieroglyph_id}') in ('419', '420'))
order by c.resource#>>'{resource,hieroglyph_id}' asc
----
SELECT jsonb_pretty(resource)
FROM ChineseRecord
WHERE resource #>> '{resource, hieroglyph_id}' = '292'
----
SELECT jsonb_pretty(resource)
FROM ChineseGrapheme
order by (resource#>>'{id}')::int
----
delete from chineserecord;
----
SELECT c.resource AS c,
(count(*) OVER()) FROM ChineseRecord AS c
WHERE (c.resource#>>'{graphemes}') is not null


-- ('水'))
--   AND (((c.resource#>>'{resource,hieroglyph_id}')::int) >= 287)
--  ORDER BY (c.resource#>>'{resource,hieroglyph_id}')::int ASC LIMIT 50 OFFSET 0
----
SELECT c.resource AS c
FROM ChineseRecord AS c

WHERE ((c.resource#>'{graphemes}') ?| array['水'])

AND (((c.resource#>>'{resource,hieroglyph_id}')::int) >= 280)
ORDER BY (c.resource#>>'{resource,hieroglyph_id}')::int ASC
----
select jsonb_pretty(resource)
from chineserecord
limit 2;
-- where resource#>>'{id}' = '119'
-- limit 1;
----
SELECT c.resource AS c
FROM chineserecord AS c
WHERE ((c.resource#>'{graphemes}' ? '纟' ))
----
SELECT c.resource AS c
FROM chineserecord AS c
WHERE ((c.resource#>'{graphemes}' is not null ))

----
select 1;
----
delete from chineserecord
----
select jsonb_pretty(resource) from  chinesegrapheme
----
\d
----
select count(*) from chineserecord;
----
SELECT c.resource AS c
FROM chinesegrapheme AS c
WHERE (c.resource#>>'{chinese,general}')  IN ('米')
ORDER BY (c.resource#>>'{id}')::int ASC
----
SELECT c.resource AS c
FROM chinesegrapheme AS c
WHERE ((c.resource#>>'{chinese,general}') IN ('米'))
ORDER BY (c.resource#>>'{id}')::int ASC LIMIT 50 OFFSET 0
----
SELECT c.resource AS c FROM chinesegrapheme AS c WHERE (c.resource#>>'{id}') = '120'
----
select jsonb_pretty(resource)
from recordgroup;
----
delete from recordgroup;
----
SELECT jsonb_pretty(c.resource#>'{resource}') AS c,
(count(*) OVER()) FROM ChineseRecord AS c
WHERE ((c.resource#>>'{resource, chinese}') IN ('千', '书') )
LIMIT 50 OFFSET 0
----


SELECT c.resource AS c
FROM ChineseRecord AS c
WHERE ((c.resource#>>'{resource,chinese}') IN (['千']))
LIMIT 50 OFFSET 0

----
select jsonb_pretty(resource)
from recordgroup

----
SELECT c.resource AS c,
(count(*) OVER()) FROM ChineseRecord AS c
WHERE (c.resource#>>'{resource,chinese}') IN ()
ORDER BY (c.resource#>>'{resource,hieroglyph_id}')::int ASC
LIMIT 50 OFFSET 0
----

select id, resource from usertag;
----
delete from usertag;
----
DELETE FROM usertag WHERE id = 'b64709d8-0514-4f78-a425-f6cb567be8e0'
----
select count(*)
from chineserecord
----
delete from app_user
----
select jsonb_pretty(resource)
from app_user;
----
select *, jsonb_pretty(resource)
from usertag limit 1;
----
delete from chineserecord
----

select jsonb_pretty(resource)
from usersubscription;
----
select *, jsonb_pretty(resource)
from usertag
----
drop table temp_b;
create table temp_b (id integer primary key, t text) ;

----
insert into temp_b (id, t) values (1, 'a')

----
INSERT INTO temp_b (id, t)
VALUES (1, 'A'), (2, 'B'), (3, 'C')
ON CONFLICT (id) DO UPDATE
  SET t = excluded.t
----
----
INSERT INTO tablename (a, b, c) values (1, 2, 10)
ON CONFLICT (a) DO UPDATE SET c = tablename.c + 1;
----
\d usertag
----
select * from temp_b;
----
SELECT
    pid
    ,datname
    ,usename
    ,application_name
    ,client_hostname
    ,client_port
    ,backend_start
    ,query_start
    ,query
    ,state
FROM pg_stat_activity
WHERE state = 'active';
----
drop table migration;
----
\d
----
select jsonb_pretty(resource)
from chinesegrapheme
limit 1;
----
select jsonb_pretty(resource)
from chineserecord
limit 1;

----
delete from chinesegrapheme;
delete from chineserecord;
----
select count(*) from chineserecord;
----
select jsonb_pretty(resource)
from chineserecord
limit 1;
-- limit 2300;
----
select jsonb_pretty(resource)
from chineserecord
where resource#>>'{resource, hieroglyph_id}' = '980'
limit 1;
----
select jsonb_pretty(resource)
from usertag
limit 10;
----
select jsonb_pretty(resource)
from usersubscription
limit 10;
----
delete from usersubscription
----
drop table car;
drop table usersubscription;
----
select * from car;
----
select * from app_user;
----

SELECT resource FROM chineserecord WHERE ((resource#>'{graphemes}' ? '亠' ))
----
SELECT resource FROM chineserecord WHERE ((resource#>'{graphemes}' @> '"亠"' ))
----
select jsonb_pretty(resource)
from chineserecord
where resource#>>'{graphemes}' is not null
limit 1;
----
SELECT c.resource AS c,
(count(*) OVER()) FROM ChineseRecord AS c
WHERE ((c.resource#>'{graphemes}' ??| array['寸'] ))
AND (((c.resource#>>'{created}') <= ?)
AND ((c.resource#>>'{created}') >= ?)) ORDER BY (c.resource#>>'{resource,hieroglyph_id}')::int ASC LIMIT ? OFFSET ?" "2022-12-18T18:33:00.050Z" "2022-11-19T18:33:00.050Z" 50 0
----
SELECT c.resource AS c, (count(*) OVER())
FROM ChineseRecord AS c
WHERE c.resource#>'{graphemes}' in ('t')
ORDER BY (c.resource#>>'{resource,hieroglyph_id}')::int ASC
LIMIT 50 OFFSET 0
----
----

SELECT c.resource#>'{graphemes}'
FROM ChineseRecord AS c
WHERE c.resource#>'{graphemes}' @> '["非", "车"]'
LIMIT 1

----
----
select jsonb_pretty(resource) from usertag;
----
delete from userlike
----
select jsonb_pretty(resource) from userlike;

----
----
select jsonb_pretty(resource) from
chineserecord
limit 1
-- where id = '15087a0f-d25b-45c6-81b7-68fa3178186e'
----
delete from usertag
----
select jsonb_pretty(resource)
from chineserecord
order by ts asc
limit 1;
----
-- VALIDATION QUERY
----
select resource#>>'{resource, hieroglyph_id}' as h,
count(*) as cnt,
jsonb_agg(resource#>>'{resource, chinese}') as c
from chineserecord
group by resource#>>'{resource, hieroglyph_id}'
----
truncate recordgroup;
----
select jsonb_pretty(resource)
from recordgroup
----
truncate CardViewConfiguration
----
truncate repetitiongroupentry
----
select id, jsonb_pretty(resource)
from CardViewConfiguration
----

\d
----
select
--count(*),
 jsonb_pretty(resource)
from repetitiongroupentry
----
SELECT repetition.resource,
coalesce((repetition.resource#>'{repetition-coefficient}')::int, 0) * interval '1 day',
LOCALTIMESTAMP >= coalesce(repetition.ts::timestamptz, c.ts::timestamp),
coalesce((repetition.resource#>'{repetition-coefficient}')::int, 0) * interval '1 day',
repetition.resource AS repetition
FROM ChineseRecord AS c
LEFT JOIN RepetitionGroupEntry AS repetition
ON ((repetition.resource#>>'{hieroglyph_id}') = (c.resource#>>'{resource,hieroglyph_id}'))
AND (((LOCALTIMESTAMP >= coalesce(repetition.ts::timestamptz, c.ts::timestamp)
 + coalesce((repetition.resource#>'{repetition-coefficient}')::int, 0) * interval '1 day')))
ORDER BY
-- (repetition.resource is not null) ASC,
coalesce((repetition.resource#>>'{repetition-coefficient}')::int, 100) ASC,
(c.resource#>>'{resource,hieroglyph_id}')::int ASC
----
select jsonb_pretty(resource)
from repetitiongroupentry

----
