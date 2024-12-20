package at.kolmann.java.FFalarmPrinter;

import de.westnordost.osmapi.map.data.Node;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Einsatz {
    private final Config config;
    private final EinsatzPDF einsatzPDF;
    private final EinsatzHTML einsatzHTML;
    private final EinsatzPrint einsatzPrint;
    private final LastEinsatzStore lastEinsatzStore;
    private final Verwaltungsgrenzen verwaltungsgrenzen;

    public Einsatz(Config config, LastEinsatzStore lastEinsatzStore) {
        this.config = config;
        this.lastEinsatzStore = lastEinsatzStore;
        einsatzPDF = new EinsatzPDF(config);
        einsatzHTML = new EinsatzHTML(config);
        einsatzPrint = new EinsatzPrint(config, lastEinsatzStore);
        verwaltungsgrenzen = new Verwaltungsgrenzen(config);
    }

    public void process(@NotNull JSONObject einsatz, String savePath, String alarmPath) throws IOException {
        String einsatzID = einsatz.getString("EinsatzID");
        System.out.println("EinsatzID: " + einsatzID);

        String einsatzAdresse = getEinsatzAdresse(einsatz);
        System.out.println("Einsatzadresse: " + einsatzAdresse);

        Double einsatzLng = null;
        Double einsatzLat = null;
        if (
                einsatz.has("Accuracy")
                && (Objects.equals(einsatz.getString("Accuracy"), "geocoded")
                    || Objects.equals(einsatz.getString("Accuracy"), "pos")
                )
        ) {
            if (einsatz.has("Lng")) {
                einsatzLng = einsatz.getDouble("Lng");
            } else {
                System.out.println("GeoCoded but Lng not found in JSON");
            }
            if (einsatz.has("Lat")) {
                einsatzLat = einsatz.getDouble("Lat");
            } else {
                System.out.println("GeoCoded but Lat not found in JSON");
            }
        }
        System.out.println("Einsatzkoordinaten: " + einsatzLat + ";" + einsatzLng);

        if (einsatzLng != null && einsatzLat != null) {
            EinsatzRouter einsatzRouter = new EinsatzRouter(config);
            JSONObject route = einsatzRouter.getRoute(einsatzLng, einsatzLat);

            JSONObject routerResult = einsatzRouter.getResult();
            String routeResultFile = savePath + File.separator + alarmPath+"_route.json";
            if (routerResult != null) {
                try (FileOutputStream fos = new FileOutputStream(routeResultFile)) {
                    System.out.println("Saving Map to file://" + routeResultFile.replace(" ", "%20"));
                    fos.write(routerResult.toString().getBytes());
                } catch (IOException e) {
                    System.out.println("Failed to save Route to file://" + routeResultFile.replace(" ", "%20"));
                }
            }

            Point einsatzLatLng = einsatzRouter.getEinsatzLatLng();
            ArrayList<Node> hydrants = null;
            List<String> verwaltungsGrenzen = null;
            if (einsatzLatLng != null) {
                GeoLocation einsatzLocation = GeoLocation.fromDegrees(einsatzLatLng.getLat(), einsatzLatLng.getLng());

                double hydrantSearchRadius = 100.0;
                try {
                    hydrantSearchRadius = config.getDouble("hydrantSearchRadius");
                } catch (IOException e) {
                    System.out.println("Failed to get hydrantSearchRadius from config: " + e.getMessage());
                }

                // Mean Earth Radius, as recommended for use by
                // the International Union of Geodesy and Geophysics,
                // see http://rosettacode.org/wiki/Haversine_formula
                double earthRadius = 6371000.0; // in meters
                GeoLocation[] einsatzBox = einsatzLocation.boundingCoordinates(hydrantSearchRadius, earthRadius);
                OsmHydrant osmHydrant = new OsmHydrant();

                hydrants = osmHydrant.getHydrants(einsatzBox);

                double verwalungsGrenzenSearchRadius = 800.0; // 800m
                try {
                    verwalungsGrenzenSearchRadius = config.getDouble("verwalungsGrenzenSearchRadius");
                } catch (IOException e) {
                    // Ignore and use default
                }
                GeoLocation[] einsatzBoxVerwaltung = einsatzLocation.boundingCoordinates(verwalungsGrenzenSearchRadius, earthRadius);
                verwaltungsGrenzen = verwaltungsgrenzen.filter(einsatzLatLng);
            }

            JSONArray hydrantsResult = new JSONArray();
            if (hydrants != null) {
                for (Node hydrant : hydrants) {
                    if (hydrant.isDeleted()) {
                        continue;
                    }

                    StringBuilder hydrantText = new StringBuilder();
                    Map<String, String> tags = hydrant.getTags();
                    if (tags.containsKey("emergency") && tags.get("emergency").equalsIgnoreCase("suction_point")) {
                        hydrantText.append("Ansaugplatz (für Pumpe)<br />");
                    } else {
                        boolean couplingTextStarted = false;
                        if (tags.containsKey("fire_hydrant:coupling_type")) {
                            hydrantText.append("Kupplung: ").append(tags.get("fire_hydrant:coupling_type"));
                            couplingTextStarted = true;
                        }
                        if (tags.containsKey("fire_hydrant:couplings")) {
                            if (!couplingTextStarted) {
                                hydrantText.append("Kupplung: ");
                                couplingTextStarted = true;
                            } else {
                                hydrantText.append(", ");
                            }
                            hydrantText.append(tags.get("fire_hydrant:couplings"));
                        }
                        if (couplingTextStarted) {
                            hydrantText.append("<br />");
                        }
                        couplingTextStarted = false;
                        if (tags.containsKey("couplings:type")) {
                            hydrantText.append("Kupplung: ").append(tags.get("couplings:type"));
                            couplingTextStarted = true;
                        }
                        if (tags.containsKey("couplings:diameters")) {
                            if (!couplingTextStarted) {
                                hydrantText.append("Kupplung: ");
                                couplingTextStarted = true;
                            } else {
                                hydrantText.append(", ");
                            }
                            hydrantText.append(tags.get("couplings:diameters"));
                        }
                        if (couplingTextStarted) {
                            hydrantText.append("<br />");
                        }

                        if (tags.containsKey("fire_hydrant:type")) {
                            switch (tags.get("fire_hydrant:type").toLowerCase()) {
                                case "pillar":
                                    hydrantText.append("Art: Überflur-Hydrant<br />");
                                    break;
                                case "underground":
                                    hydrantText.append("Art: Unterflur-Hydrant<br />");
                                    break;
                                case "wall":
                                    hydrantText.append("Art: Wandanschluss<br />");
                                    break;
                                case "pipe":
                                    hydrantText.append("Art: Steigleitung<br />");
                                    break;
                            }
                        }
                    }

                    hydrantsResult.put(
                        new JSONObject()
                                .put("Lat", hydrant.getPosition().getLatitude())
                                .put("Lon", hydrant.getPosition().getLongitude())
                                .put("text", hydrantText.toString())
                    );
                }

                // Store Verwaltungsgrenzen
                if (verwaltungsGrenzen != null) {
                    String verwaltungsGrenzenResultFile = savePath + File.separator + alarmPath+"_verwaltungsgrenzen.jsonl";
                    try (FileOutputStream fos = new FileOutputStream(verwaltungsGrenzenResultFile)) {
                        System.out.println("Saving Verwaltungsgrenzen to file://" + verwaltungsGrenzenResultFile.replace(" ", "%20"));
                        for (String line : verwaltungsGrenzen) {
                            fos.write(line.getBytes());
                            fos.write("\n".getBytes());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                String hydrantsResultFile = savePath + File.separator + alarmPath+"_hydrants.json";
                try (FileOutputStream fos = new FileOutputStream(hydrantsResultFile)) {
                    System.out.println("Saving Hydrants to file://" + hydrantsResultFile.replace(" ", "%20"));
                    fos.write(hydrantsResult.toString().getBytes());
                } catch (IOException e) {
                    System.out.println("Failed to save Hydrants to file://" + hydrantsResultFile.replace(" ", "%20"));
                }
            }

            byte[] einsatzMap = einsatzRouter.getMapsImage(hydrants);
            if (einsatzMap != null) {
                try (FileOutputStream fos = new FileOutputStream(savePath + File.separator + alarmPath+".png")) {
                    System.out.println("Saving Map to file://" + savePath.replace(" ", "%20")
                            + File.separator + alarmPath.replace(" ", "%20" + ".png")
                    );
                    fos.write(einsatzMap);
                } catch (IOException e) {
                    System.out.println("Failed to save Map to file://" + savePath.replace(" ", "%20")
                            + File.separator + alarmPath.replace(" ", "%20" + ".png")
                    );
                }
            } else {
                System.out.println("Failed to get MapsImage!");
            }

            // Calculate Disponitionen
            JSONArray disponierteFF = null;
            if (einsatz.get("Dispositionen") != null && einsatz.getJSONArray("Dispositionen").length() > 1) {
                JSONArray dispos = einsatz.getJSONArray("Dispositionen");

                // Get all still active ones and sort by DispoTime
                disponierteFF = new JSONArray();

                ArrayList<JSONObject> jsonValues = new ArrayList<>();
                for (int i = 0; i < dispos.length(); i++) {
                    jsonValues.add(dispos.getJSONObject(i));
                }
                jsonValues.sort(new Comparator<>() {
                    private static final String DISPO_KEY = "DispoTime";
                    private static final String AUS_KEY = "AusTime";

                    @Override
                    public int compare(JSONObject a, JSONObject b) {
                        String valA;
                        String valB;

                        try {
                            if (a.has(DISPO_KEY)) {
                                valA = a.getString(DISPO_KEY);
                            } else if (a.has(AUS_KEY)) {
                                valA = a.getString(AUS_KEY);
                            } else {
                                return 1;
                            }
                            if (b.has(DISPO_KEY)) {
                                valB = b.getString(DISPO_KEY);
                            } else if (b.has(AUS_KEY)) {
                                valB = b.getString(AUS_KEY);
                            } else {
                                return -1;
                            }
                        } catch (JSONException e) {
                            System.out.println("JSONException: " + e.getMessage());
                            return 1;
                        }

                        return valA.compareTo(valB);
                    }
                });

                for (int i = 0; i < dispos.length(); i++) {
                    disponierteFF.put(jsonValues.get(i));
                }
            }

            System.out.println("EinsatzAdresse: " + getEinsatzAdresse(einsatz));

            einsatzPDF.saveEinsatz(
                    savePath + File.separator + alarmPath+".pdf",
                    einsatzID,
                    einsatz,
                    disponierteFF,
                    getEinsatzAdresse(einsatz),
                    route,
                    einsatzRouter.getRouteDetails(),
                    einsatzMap
            );

            assert routerResult != null;
            einsatzHTML.saveEinsatz(
                    savePath + File.separator + alarmPath + ".html",
                    einsatzID,
                    einsatz,
                    disponierteFF,
                    getEinsatzAdresse(einsatz),
                    einsatzLng,
                    einsatzLat,
                    routerResult.toString(),
                    hydrantsResult.toString(),
                    einsatzRouter.getRouteDetails().toString()
            );

            try {
                einsatzPrint.process(einsatzID, einsatz, savePath + File.separator + alarmPath+".pdf");
            } catch (IOException | PrinterException e) {
                System.out.println("einsatzPrint Error: " + e.getMessage());
            }

            if (!lastEinsatzStore.contains(einsatzID)) {
                System.out.println("Adding to lastEinsatzStore: " + einsatzID);
                lastEinsatzStore.add(einsatzID);
            }

            einsatzRouter.shutdown();
        }

    }

    private String getEinsatzAdresse(JSONObject einsatz) {
        StringBuilder einsatzAdresse = new StringBuilder();
        if (einsatz.has("Strasse") && einsatz.getString("Strasse").contains("A2")) {
            einsatzAdresse.append("A2, 2824 Seebenstein, Austria");
        } else {
            if (einsatz.has("Objekt")) {
                einsatzAdresse.append(einsatz.getString("Objekt"));
            }
            if (einsatz.has("Strasse")) {
                if (einsatzAdresse.length() > 0) {
                    einsatzAdresse.append(System.lineSeparator());
                }
                einsatzAdresse.append(einsatz.getString("Strasse"));
                if (einsatz.has("Nummer1")) {
                    einsatzAdresse.append(" ");
                    einsatzAdresse.append(einsatz.getString("Nummer1").replace(".000", ""));
                }
            }
            if (einsatz.has("Plz")) {
                if (einsatzAdresse.length() > 0) {
                    einsatzAdresse.append(System.lineSeparator());
                }
                einsatzAdresse.append(einsatz.getString("Plz"));
                if (einsatz.has("Ort")) {
                    einsatzAdresse.append(" ");
                    einsatzAdresse.append(einsatz.getString("Ort"));
                }
            }
        }
        return einsatzAdresse.toString();
    }
}
