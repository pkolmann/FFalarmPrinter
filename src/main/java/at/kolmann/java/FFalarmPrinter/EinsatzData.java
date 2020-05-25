package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;

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

    public void process(JSONArray einsatzData) {
        Calendar myCal = Calendar.getInstance();
        String yearString = String.format(("%tY"), myCal);
        String alarmString = String.format(("alarm-%tY%<tm%<td-%<tH%<tM%<tS"), myCal);
        String savePath = config.getString("saveWEBlocation");

        String einsatzDataHashCode = String.valueOf(einsatzData.toString().hashCode());
        if (einsatzDataHashCode.equals(lastEinsatzStore.getLastEinsatzHash())) {
            // No changes since last run. No need to regenerate data
            return;
        }
        lastEinsatzStore.setLastEinsatzHash(einsatzDataHashCode);

        if (savePath != null && einsatzData.length() > 0) {
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

        // generate new archiv.html page
        archivePageGenerator.generate();

        if (einsatzData.length() == 0) {
            lastEinsatzStore.clear();
        }

        // store lastEinsatzStore
        lastEinsatzStore.run();
    }
}
