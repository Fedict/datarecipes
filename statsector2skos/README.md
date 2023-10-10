# Convert statistical sectors to SKOS

## Description

Converts Statbel Statistical sectors (NIS9) shapefile into a SKOS RDF file (JSON-LD, Turtle or NTriples).
The Lambert 2008 coordinates are converted into ETRS89.

## Sources

See also https://statbel.fgov.be/en/open-data/statistical-sectors-2023

## Running

Download and unzip the source files.

Compile the tool using maven and JDK 17.

``` java -jar statsector2skos-1.0-SNAPSHOT.jar <input.shp> <base-uri> <output.ttl> ```
