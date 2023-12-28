package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.Calendar;

public class EinsatzData {
    final Config config;
    final Einsatz einsatz;
    private final LastEinsatzStore lastEinsatzStore;
    private final ArchivePageGenerator archivePageGenerator;

    public EinsatzData(Config config) {
        this.config = config;
        this.lastEinsatzStore = new LastEinsatzStore(config);
        this.einsatz = new Einsatz(config, lastEinsatzStore);
        this.archivePageGenerator = new ArchivePageGenerator(config);
    }

    public void process(JSONObject einsatzJSON) {
        Calendar myCal = Calendar.getInstance();
        String yearString = String.format(("%tY"), myCal);
        String alarmString = String.format(("alarm-%tY%<tm%<td-%<tH%<tM%<tS"), myCal);
        String savePath = config.getString("saveWEBlocation");

        JSONArray einsatzData = einsatzJSON.getJSONArray("EinsatzData");

        String einsatzJSONHashCode = String.valueOf(einsatzJSON.toString().hashCode());
        if (einsatzJSONHashCode.equals(lastEinsatzStore.getLastEinsatzHash())) {
            // No changes since last run. No need to regenerate data
            return;
        }
        lastEinsatzStore.setLastEinsatzHash(einsatzJSONHashCode);

        if (!einsatzData.isEmpty()) {
            // Print current date for logile
            System.out.printf("%tY-%<tm-%<td %<tH:%<tM:%<tS%n", myCal);
        }

        if (savePath != null && !einsatzData.isEmpty()) {
            savePath += File.separator + yearString;
            File savePathFile = new File(savePath);
            if (!savePathFile.isAbsolute()) {
                savePath = System.getProperty("user.dir") + File.separator + savePath;
                savePathFile = new File(savePath);
            }

            if (!savePathFile.exists()) {
                if (!savePathFile.mkdirs()) {
                    System.out.println("Failed to create "+ savePathFile);
                }
            }

            if (savePathFile.exists()) {
                File saveJSONfile = new File(savePath + File.separator + alarmString + ".json");

                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(saveJSONfile));
                    writer.write(einsatzJSON.toString(2));
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
            String alarmPath = alarmString + "-" +
                    einsatzData.getJSONObject(i).getString("EinsatzID");
            try {
                einsatz.process(einsatzData.getJSONObject(i), savePath, alarmPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("========");
        }

        // generate new archiv.html page
        archivePageGenerator.generate();

        if (einsatzData.isEmpty()) {
            lastEinsatzStore.clear();
        }

        // store lastEinsatzStore
        lastEinsatzStore.run();
    }
}
