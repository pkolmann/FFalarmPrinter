package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;

public class Einsatz {
    private Config config;

    public Einsatz(Config config) {
        this.config = config;
    }

    public int process(JSONObject einsatz, String alarmPath) {
        String einsatzID = einsatz.getString("EinsatzID");
        System.out.println(einsatzID);

        StringBuilder einsatzAdresse = new StringBuilder();
        if (einsatz.has("Strasse")) {
            einsatzAdresse.append(einsatz.getString("Strasse"));
            if (einsatz.has("Nummer1")) {
                einsatzAdresse.append(" ");
                einsatzAdresse.append(einsatz.getString("Nummer1"));
            }
        }
        if (einsatz.has("Plz")) {
            if (einsatzAdresse.length() > 0) {
                einsatzAdresse.append(", ");
            }
            einsatzAdresse.append(einsatz.getString("Plz"));
            if (einsatz.has("Ort")) {
                einsatzAdresse.append(" ");
                einsatzAdresse.append(einsatz.getString("Ort"));
            }
        }

        if (einsatzAdresse.length() > 0) {
            EinsatzRouter einsatzRouter = new EinsatzRouter(config);
            einsatzRouter.getRoute(einsatzAdresse.toString());

            ImageResult einsatzMap = einsatzRouter.getMapsImage();
            if (einsatzMap != null) {
                try (FileOutputStream fos = new FileOutputStream(alarmPath+".png")) {
                    System.out.println("Saving Map to " + alarmPath + ".png");
                    fos.write(einsatzMap.imageData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            einsatzRouter.shutdown();
        }

        return 0;
    }

    public void shutdown() {
    }
}
