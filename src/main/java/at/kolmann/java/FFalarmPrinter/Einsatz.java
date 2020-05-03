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

        einsatzRouter.getRoute("Untere Feldstrasse 80, 2823 Pitten");





        return 0;
    }

    public void finish() {
        einsatzRouter = null;
    }
}
