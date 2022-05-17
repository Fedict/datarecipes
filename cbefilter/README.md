# Filter entities in Crossroadbank Enterprises by NACE (activity) code)

Register (for free) via the [CBE open data](https://economie.fgov.be/en/themes/enterprises/crossroads-bank-enterprises/services-everyone/cbe-open-data) page
to obtain a login.

Download the full KBO download ZIP and extract the files to a local directory.

Load the files into e.g. an [H2](http://www.h2database.com/) local database, using the [CSVREAD](http://www.h2database.com/html/functions.html#csvread) command.

Set an index on various columns (entitynumber / establishmentnumber)

Execute the SQL script

```
create table results as
select distinct e.enterprisenumber as parentnumber,
(select d.denomination from denomination d where language='2' and d.entitynumber = e.enterprisenumber and typeofdenomination='001') as parentnamenl, 
(select d.denomination from denomination d where language='1' and d.entitynumber = e.enterprisenumber and typeofdenomination='001') as parentnamefr,
a.entitynumber,
(select d.denomination from denomination d where language='2' and d.entitynumber = a.entitynumber and typeofdenomination='001') as namenl, 
(select d.denomination from denomination d where language='1' and d.entitynumber = a.entitynumber and typeofdenomination='001') as namefr,
(select group_concat(a2.nacecode) from activity a2 where a.entitynumber = a2.entitynumber and a2.naceversion='2008') as nacecodes, 
extraaddressinfo, streetnl, streetfr, housenumber, box, zipcode, municipalitynl, municipalityfr 
from activity a 
inner join denomination d on d.entitynumber = a.entitynumber
inner join address addr on addr.entitynumber = a.entitynumber
left join establishment e on e.establishmentnumber = a.entitynumber
where naceversion = '2008' and left(nacecode, 2) = '84';
```
