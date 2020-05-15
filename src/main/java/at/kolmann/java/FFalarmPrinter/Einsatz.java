package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import com.google.maps.model.*;
import org.json.JSONObject;

import javax.print.PrintException;
import java.io.FileOutputStream;
import java.io.IOException;

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

    public void process(JSONObject einsatz, String alarmPath) {
        String einsatzID = einsatz.getString("EinsatzID");
        System.out.println(einsatzID);

        String einsatzAdresse = getEinsatzAdresse(einsatz);

        if (einsatzAdresse.length() > 0) {
            EinsatzRouter einsatzRouter = new EinsatzRouter(config);
            //DirectionsRoute route = null;
            DirectionsRoute route = einsatzRouter.getRoute(einsatzAdresse);

            ImageResult einsatzMap = einsatzRouter.getMapsImage();
            if (einsatzMap != null) {
                try (FileOutputStream fos = new FileOutputStream(alarmPath+".png")) {
                    System.out.println("Saving Map to " + alarmPath + ".png");
                    fos.write(einsatzMap.imageData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            einsatzPDF.saveEinsatz(
                    alarmPath+".pdf",
                    einsatzID,
                    einsatz,
                    getEinsatzAdresse(einsatz),
                    route,
                    einsatzMap
            );

            einsatzHTML.saveEinsatz(
                    alarmPath + ".html",
                    einsatzID,
                    einsatz,
                    getEinsatzAdresse(einsatz),
                    route,
                    einsatzMap
            );

            try {
                einsatzPrint.process(einsatzID,alarmPath+".pdf");
            } catch (IOException | PrintException e) {
                e.printStackTrace();
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
                    einsatzAdresse.append(einsatz.getString("Nummer1"));
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
