package at.kolmann.java.FFalarmPrinter;

import org.json.JSONObject;

public class Einsatz {
    private Config config;
    private EinsatzRouter einsatzRouter;

    public Einsatz(Config config) {
        this.config = config;
        einsatzRouter = new EinsatzRouter(config);
    }

    public int process(JSONObject einsatz) {
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
            einsatzRouter.getRoute(einsatzAdresse.toString());
        }






        return 0;
    }

    public void shutdown() {
        einsatzRouter.shutdown();
    }
}
