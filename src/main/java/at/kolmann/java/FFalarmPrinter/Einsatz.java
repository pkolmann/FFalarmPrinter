package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import com.google.maps.model.*;
import de.westnordost.osmapi.map.data.Node;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.print.PrintException;
import java.awt.print.PrinterException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class Einsatz {
    private final Config config;
    private final EinsatzPDF einsatzPDF;
    private final EinsatzHTML einsatzHTML;
    private final EinsatzPrint einsatzPrint;
    private final LastEinsatzStore lastEinsatzStore;

    public Einsatz(Config config, LastEinsatzStore lastEinsatzStore) {
        this.config = config;
        this.lastEinsatzStore = lastEinsatzStore;
        einsatzPDF = new EinsatzPDF(config);
        einsatzHTML = new EinsatzHTML(config);
        einsatzPrint = new EinsatzPrint(config, lastEinsatzStore);
    }

    public void process(JSONObject einsatz, String alarmPath) throws IOException {
        String einsatzID = einsatz.getString("EinsatzID");
        System.out.println(einsatzID);

        String einsatzAdresse = getEinsatzAdresse(einsatz);
        System.out.println("Einsatzadresse: " + einsatzAdresse);

        if (einsatzAdresse.length() > 0) {
            EinsatzRouter einsatzRouter = new EinsatzRouter(config);
            //DirectionsRoute route = null;
            DirectionsRoute route = einsatzRouter.getRoute(einsatzAdresse);

            LatLng einsatzLatLng = einsatzRouter.getEinsatzLatLng();
            ArrayList<Node> hydrants = null;
            if (einsatzLatLng != null) {
                GeoLocation einsatzLocation = GeoLocation.fromDegrees(einsatzLatLng.lat, einsatzLatLng.lng);

                double hydrantSearchRadius = 100.0;
                try {
                    hydrantSearchRadius = config.getDouble("hydrantSearchRadius");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Mean Earth Radius, as recommended for use by
                // the International Union of Geodesy and Geophysics,
                // see http://rosettacode.org/wiki/Haversine_formula
                double earthRadius = 6371000.0; // in meters
                GeoLocation[] einsatzBox = einsatzLocation.boundingCoordinates(hydrantSearchRadius, earthRadius);
                OsmHydrant osmHydrant = new OsmHydrant();

                hydrants = osmHydrant.getHydrants(einsatzBox);
            }

            byte[] einsatzMap = einsatzRouter.getMapsImage(hydrants);
            if (einsatzMap != null) {
                try (FileOutputStream fos = new FileOutputStream(alarmPath+".png")) {
                    System.out.println("Saving Map to " + alarmPath + ".png");
                    fos.write(einsatzMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                    private static final String KEY_NAME = "DispoTime";

                    @Override
                    public int compare(JSONObject a, JSONObject b) {
                        String valA = "";
                        String valB = "";

                        try {
                            valA = a.getString(KEY_NAME);
                            valB = b.getString(KEY_NAME);
                        } catch (JSONException e) {
                            e.printStackTrace();
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
                    alarmPath+".pdf",
                    einsatzID,
                    einsatz,
                    disponierteFF,
                    getEinsatzAdresse(einsatz),
                    route,
                    einsatzMap
            );

            einsatzHTML.saveEinsatz(
                    alarmPath + ".html",
                    einsatzID,
                    einsatz,
                    disponierteFF,
                    getEinsatzAdresse(einsatz),
                    hydrants
            );

            try {
                einsatzPrint.process(einsatzID,alarmPath+".pdf");
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
