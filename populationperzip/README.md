# Create a list of inhabitants per postal codes

## Description

The Belgian Statistics Office publishes population statistics per "statistical sector" and a map of statistical sectors in various (geo-)formats.
A Belgian municipality is comprised of one or more statistical sectors.

However, these sectors are not postal districts: the Belgian Post (bPost) maintains the list of postal codes and the geographic area they apply to.
There is no one-to-one relation between postal codes and municipalities or statistical sector.

## Sources

- [Postal Districts](https://www.geo.be/catalog/details/9738c7c0-5255-11ea-8895-34e12d0f0423?l=en) (SHP)
- [Population per statistical sector](https://statbel.fgov.be/en/open-data/statistical-sectors-2020) (TXT/CSV)
- [Statistical Sectors](https://statbel.fgov.be/en/open-data/statistical-sectors-2020) (GeoJSON)

## Main tool

[GeoTools](https://geotools.org/) open source java library.

## Approach

To get the postal code for a given sector: calculate the "innerpoint" of the sector and check if this point is within the boundaries of a postal code.
Combine the data based on the statistical sector ID.

## Notes

- Different postal districts may have the same postal code (i.e. postal code is not unique within the Postal Districts file)
- About 6.000 persons in the population file are not "assigned" to a specific statistical sector, they are assigned to a sector ending with "ZZZZ"
- Geotools failed to process the SHP version of the Statistical Sectors file, hence the GeoJSON was used
