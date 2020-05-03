package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class FFalarmPrinter {
    private Florian10Fetcher florian10Fetcher;
    private Config config;
    EinsatzData einsatzData;

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

    public void run() {
        String urlToFetch = null;
        if (config.get("wastlUrl") instanceof String) {
            urlToFetch = (String) config.get("wastlUrl");
        }
        if (urlToFetch == null) {
            urlToFetch = "https://infoscreen.florian10.info/ows/infoscreen/einsatz.ashx";
        }

        JSONObject florian10Data = florian10Fetcher.fetchFlorian10Data(urlToFetch);

        if (florian10Data == null) {
            System.out.println("Fetching Data failed....");
        } else {
            if (florian10Data.has("error")) {
                System.out.println("Failed to fetch data:");
                System.out.println("");
                System.out.println(florian10Data.toString(2));
                System.out.println("");
            } else if (florian10Data.has("CurrentState")) {
                if (florian10Data.get("CurrentState").equals("waiting") && florian10Data.has("Token")) {
                    System.out.println("Token muss erst freigeschalten werden:");
                    System.out.println("");
                    System.out.println(florian10Data.get("Token"));
                    System.out.println("");
                }

                if (florian10Data.getString("CurrentState").equals("data") && florian10Data.has("EinsatzData")) {
                    einsatzData.process(florian10Data.getJSONArray("EinsatzData"));
                }
            } else {
                System.out.println("No 'CurrentState' found in Respnse!");
                System.out.println("");
                System.out.println("Data from Florian 10:");
                System.out.println(florian10Data.toString(2));
                System.out.println("");
            }

        }
    }
}
