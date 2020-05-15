package at.kolmann.java.FFalarmPrinter;

import java.io.*;
import java.util.ArrayList;

public class LastEinsatzStore implements Runnable {
    private final ArrayList<String> lastEinsatz = new ArrayList<>();
    private String lastEinsatzPath;

    public LastEinsatzStore(Config config) {

        lastEinsatzPath = config.getString("lastEinsatzFile");
        if (lastEinsatzPath == null) {
            return;
        }

        File lastEinsatzFile = new File(lastEinsatzPath);
        if (!lastEinsatzFile.isAbsolute()) {
            lastEinsatzPath = System.getProperty("user.dir") + File.separator + lastEinsatzPath;
            lastEinsatzFile = new File(lastEinsatzPath);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this));

        if (!lastEinsatzFile.exists()) {
            return;
        }

        try {
            String line;

            InputStreamReader inputreader = new InputStreamReader(new FileInputStream(lastEinsatzFile));
            BufferedReader bufferedReader = new BufferedReader(inputreader);
            while ((line = bufferedReader.readLine()) != null) {
                if (!lastEinsatz.contains(line)) {
                    lastEinsatz.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void add(String einsatz) {
        if (! lastEinsatz.contains(einsatz)) {
            lastEinsatz.add(einsatz);
        }
    }

    public Boolean contains(String einsatz) {
        return lastEinsatz.contains(einsatz);
    }

    public void clear() {
        lastEinsatz.clear();
    }

    public ArrayList<String> get() {
        return lastEinsatz;
    }


    @Override
    public void run() {
        File file = new File(lastEinsatzPath);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String einEinsatz : lastEinsatz) {
            System.out.println("Writing to lastEinsatzFile: " + einEinsatz);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
                writer.write(einEinsatz);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
