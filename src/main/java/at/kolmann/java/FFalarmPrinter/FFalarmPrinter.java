package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;


public class FFalarmPrinter {
    private final Florian10Fetcher florian10Fetcher;
    private final Config config;
    private final EinsatzData einsatzData;

    public FFalarmPrinter() {
        config = new Config();
        String cookieFile = null;
        if (config.get("cookieFile") instanceof String) {
            cookieFile = (String) config.get("cookieFile");
        }
        if (cookieFile == null) {
            cookieFile = "cookies.txt";
        }
        florian10Fetcher = new Florian10Fetcher(cookieFile);
        einsatzData = new EinsatzData(config);
    }

    public void run(String inputFileString) {
//        System.out.println(System.getProperty("os.name"));
        Calendar myCal = Calendar.getInstance();

        String urlToFetch = null;
        JSONObject florian10Data = null;
        if (inputFileString != null) {
            File inputFile = new File(inputFileString);

            if (!inputFile.isAbsolute()) {
                inputFile = new File(System.getProperty("user.dir") + File.separator + inputFileString);
            }

            if (inputFile.exists()) {
                // If file exists, use it.
                StringBuilder inputString = Tools.readFile(inputFile);
                florian10Data = new JSONObject(inputString.toString());
            } else {
                urlToFetch = inputFileString;
            }
        }

        if (florian10Data == null) {
            if (config.get("wastlUrl") instanceof String) {
                urlToFetch = config.getString("wastlUrl");
            }
            if (urlToFetch == null) {
                urlToFetch = "https://infoscreen.florian10.info/ows/infoscreen/einsatz.ashx";
            }
            florian10Data = florian10Fetcher.fetchFlorian10Data(urlToFetch);
        }

        if (florian10Data == null) {
            System.out.printf("%tY-%<tm-%<td %<tH:%<tM:%<tS%n", myCal);
            System.out.println("Fetching Data failed....");
        } else {
            if (florian10Data.has("error")) {
                System.out.printf("%tY-%<tm-%<td %<tH:%<tM:%<tS%n", myCal);
                System.out.println("Failed to fetch data:");
                System.out.println();
                System.out.println(florian10Data.toString(2));
                System.out.println();
            } else if (florian10Data.has("CurrentState")) {
                if (
                        (florian10Data.get("CurrentState").equals("token") ||
                                florian10Data.get("CurrentState").equals("waiting")
                        ) && florian10Data.has("Token")
                ) {
                    System.out.printf("%tY-%<tm-%<td %<tH:%<tM:%<tS%n", myCal);
                    System.out.println("Token muss erst freigeschalten werden:");
                    System.out.println();
                    System.out.println(florian10Data.get("Token"));
                    System.out.println();
                } else if (florian10Data.getString("CurrentState").equals("data") && florian10Data.has("EinsatzData")) {
                    // Process data
                    einsatzData.process(florian10Data, inputFileString);
                } else if (florian10Data.getString("CurrentState").equals("error")) {
                    System.out.printf("%tY-%<tm-%<td %<tH:%<tM:%<tS%n", myCal);
                    System.out.println("Fehler von Florian Krems gemeldet:");
                    JSONArray einsatzErrors = florian10Data.getJSONArray("Errors");
                    for (int i = 0; i < einsatzErrors.length(); i++) {
                        JSONObject einsatzError = einsatzErrors.getJSONObject(i);
                        System.out.print("  * " + einsatzError.getString("ErrorMsg"));
                        System.out.print(" (Error Code: " + einsatzError.getInt("ErrorCode"));
                        if (einsatzError.getBoolean("IsCritical")) {
                            System.out.print(", KRITISCHER FEHLER!");
                        }
                        System.out.println(")");
                    }
                    System.out.println();
                } else {
                    System.out.printf("%tY-%<tm-%<td %<tH:%<tM:%<tS%n", myCal);
                    System.out.println("Unknown CurrentState!");
                    System.out.println();
                    System.out.println();
                    System.out.println(florian10Data.toString(2));
                    System.out.println();
                }
            } else {
                System.out.printf("%tY-%<tm-%<td %<tH:%<tM:%<tS%n", myCal);
                System.out.println("No 'CurrentState' found in Response!");
                System.out.println();
                System.out.println("Data from Florian 10:");
                System.out.println(florian10Data.toString(2));
                System.out.println();
            }

        }
    }
}
