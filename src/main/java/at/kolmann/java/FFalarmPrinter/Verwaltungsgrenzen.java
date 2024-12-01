package at.kolmann.java.FFalarmPrinter;


import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geojson.feature.FeatureJSON;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Verwaltungsgrenzen {
    private final Config config;

    public Verwaltungsgrenzen(Config config) {
        this.config = config;
    }

    // Filter the GeoJSONL file by bounding box and return the filtered lines
    public List<String> filter(Point einsatzLatLng) {
        List<String> filteredLines = new ArrayList<>();

        System.out.println(
            "Filtering GeoJSONL file by Einsatz location ("+ einsatzLatLng.toString() + ")"
        );

        try {
            String filePath = config.getString("verwaltungsGrenzenFile");

            if (filePath == null) {
                System.out.println("No verwaltungsGrenzenFile set in config.json. (Set to 'none' to hide this message)");
                return null;
            }

            if (filePath.equalsIgnoreCase("none")) {
                return null;
            }

            File verwaltungsGrenzenFile = new File(filePath);
            if (!verwaltungsGrenzenFile.isAbsolute()) {
                filePath = System.getProperty("user.dir") + File.separator + filePath;
                verwaltungsGrenzenFile = new File(filePath);
            }

            if (!verwaltungsGrenzenFile.exists()) {
                System.out.println("VerwaltungsGrenzenFile " + filePath + " does not exist!");
                return null;
            }

            // Read the GeoJSONL file
            BufferedReader reader = new BufferedReader(new FileReader(verwaltungsGrenzenFile));

            // Iterate over the lines of the GeoJSONL file
            String line;
            while ((line = reader.readLine()) != null) {
                // Decode JSON line
                FeatureJSON featureJSON = new FeatureJSON();
                SimpleFeature feature = featureJSON.readFeature(line);

                // Check if the feature is within the bounding box
                if (feature.getBounds().contains(einsatzLatLng.getLng(), einsatzLatLng.getLat())) {
                    filteredLines.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Filtered " + filteredLines.size() + " features");

        return filteredLines;
    }
}