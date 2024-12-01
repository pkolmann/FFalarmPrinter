# at.kolmann.java.FFalarmPrinter.FFalarmPrinter
Florian 10 WASTL alarm printer

We used to have a perl program, fetching the data from Florian 10 WASTL interface and create a nice HTML page to print.

I now want to redo this work in java to have it available for Windows and Linux.

I have this here on GitHub so others can use this too.

<b>!!!! WARNING !!!</b>

This is my first Java project, so things are not perfect. Please don't be too hard with your critism.

## Compile

This project uses maven. So you should be able to compile it via

`mvn package assembly:single`

## Getting Verwaltungsgrenzen

The Verwaltungsgrenzen are fetched from the Verwaltungsgrenzen product from the BEV.

https://www.bev.gv.at/Services/Produkte/Kataster-und-Verzeichnisse/Verwaltungsgrenzen.html

https://www.bev.gv.at/Services/Downloads/Produktbezogene-Downloads/Unentgeltliche-Produkte/Kataster-Verzeichnisse/Aktualitaetsstand-2021.html

Then open the whole zip file as Layer in QGIS and export it as GeoJSON.

* Add Vector Layer (file.zip)
* Export
* Save Features as
* GeoJSON - Newline Delimited
* File: Verwaltungsgrenzen.geojsonl
* CRS: EPSG:4326
* Export all features
* Save

## Running

It's important to have the current working directory set to the proper directory or it won't work.

On linux I have it running via:

`cd $HOME/FFalarmPrinter; java -jar target/FFalarmPrinter-2023.12.27-jar-with-dependencies.jar`


