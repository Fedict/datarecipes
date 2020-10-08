# AdminVector2CSV

Converts the [AdminVector](https://www.geo.be/catalog/details/fb1e2993-2020-428c-9188-eb5f75e284b9?l=en) shapefile into an ordered CSV list with the names 
of the municipalities + the geographical coordinates of the municipality center.

It uses the shapefile with WGS82 ("GPS") coordinates, and creates a ";" separated CSV which should be readable in e.g. MS-Excel.
An example can be found [here](/example).

Tested with AdoptOpenJDK Java 11 on Windows.
