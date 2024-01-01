package at.kolmann.java.FFalarmPrinter;

import java.io.*;

public class Tools {
    static StringBuilder readFile(File configFile) {
        StringBuilder configString = new StringBuilder();

        try {
            InputStreamReader inputreader = new InputStreamReader(new FileInputStream(configFile));
            BufferedReader bufferedReader = new BufferedReader(inputreader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                configString.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return configString;
    }

}
