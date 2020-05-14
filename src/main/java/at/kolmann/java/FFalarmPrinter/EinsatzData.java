package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;

import java.io.*;
import java.util.Calendar;

public class EinsatzData {
    final Config config;
    final Einsatz einsatz;

    public EinsatzData(Config config) {
        this.config = config;
        this.einsatz = new Einsatz(config);
    }

    public void process(JSONArray einsatzData) {
        Calendar myCal = Calendar.getInstance();
        String yearString = String.format(("%tY"), myCal);
        String alarmString = String.format(("alarm-%tY%<tm%<td-%<tH%<tM%<tS"), myCal);
        String savePath = config.getString("saveWEBlocation");

        if (savePath != null) {
            savePath += File.separator + yearString;
            File savePathFile = new File(savePath);
            if (!savePathFile.isAbsolute()) {
                savePath = System.getProperty("user.dir") + File.separator + savePath;
                savePathFile = new File(savePath);
            }

            if (!savePathFile.exists()) {
                if (!savePathFile.mkdirs()) {
                    System.out.println("Failed to create "+savePathFile.toString());
                }
            }

            if (savePathFile.exists()) {
                File saveJSONfile = new File(savePath + File.separator + alarmString + ".json");

                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(saveJSONfile));
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
            String alarmPath = savePath + File.separator + alarmString + "-" +
                    einsatzData.getJSONObject(i).getString("EinsatzID");
            einsatz.process(einsatzData.getJSONObject(i), alarmPath);
            System.out.println("========");
        }

    }
}
