package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.Calendar;

public class EinsatzData {
    Config config;
    Einsatz einsatz;

    public EinsatzData(Config config) {
        this.config = config;
        this.einsatz = new Einsatz(config);
    }

    public int process(JSONArray einsatzData) {
        Calendar myCal = Calendar.getInstance();

        String saveJSONlocation = null;
        if (config.get("saveJSONlocation") instanceof String) {
            saveJSONlocation = (String) config.get("saveJSONlocation");
        }
        if (saveJSONlocation != null) {
            File savePath = new File(saveJSONlocation);
            if (!savePath.isAbsolute()) {
                savePath = new File(System.getProperty("user.dir") + File.separator + savePath);
            }

            if (!savePath.exists()) {
                if (!savePath.mkdirs()) {
                    System.out.println("Failed to create "+savePath.toString());
                }
            }

            if (savePath.exists()) {
                String fileName = String.format(("alarm-%tY%<tm%<td-%<tH%<tM%<tS.json"), myCal);
                File saveFile = new File(savePath + File.separator + fileName);

                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile));
                    writer.write(einsatzData.toString(2));
                    writer.newLine();
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        for (int i = 0; i < einsatzData.length(); i++) {
            System.out.println("Einsatz Daten " + i + ":");
            einsatz.process(einsatzData.getJSONObject(i));
            System.out.println("========");
        }

        System.out.println("EinsatzData end...");
        return 0;
    }

    public void shutdown() {
        einsatz.shutdown();
    }
}
