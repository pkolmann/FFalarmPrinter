package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void process(JSONObject einsatzJSON, String inputFileString) {
        Calendar myCal = Calendar.getInstance();

        if (inputFileString != null) {
            // Check if we have a date coded in the file to start again from
            Pattern pattern = Pattern.compile(".*alarm-([0-9]{8})-([0-9]{6}).json$", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(inputFileString);
            if(matcher.find()) {
                if (matcher.groupCount() == 2) {
                    int year = Integer.parseInt(matcher.group(1).substring(0, 4));
                    int month = Integer.parseInt(matcher.group(1).substring(4, 6));
                    int day = Integer.parseInt(matcher.group(1).substring(6, 8));
                    int hour = Integer.parseInt(matcher.group(2).substring(0, 2));
                    int min = Integer.parseInt(matcher.group(2).substring(2, 4));
                    int sec = Integer.parseInt(matcher.group(2).substring(4, 6));
                    myCal = new Calendar.Builder().setCalendarType("gregory")
                            .setDate(year, (month - 1), day)
                            .setTimeOfDay(hour, min, sec)
                            .build();
                }
            }
        }

        String yearString = String.format(("%tY"), myCal);
        String alarmString = String.format(("alarm-%tY%<tm%<td-%<tH%<tM%<tS"), myCal);
        String savePath = config.getString("saveWEBlocation");

        JSONArray einsatzData = einsatzJSON.getJSONArray("EinsatzData");

        String einsatzJSONHashCode = String.valueOf(einsatzJSON.toString().hashCode());
        // always recreate if inputFileString is given
        if (inputFileString == null && einsatzJSONHashCode.equals(lastEinsatzStore.getLastEinsatzHash())) {
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
