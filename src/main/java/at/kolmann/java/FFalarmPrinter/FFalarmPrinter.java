package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class FFalarmPrinter {
    private Florian10Fetcher florian10Fetcher;

    public FFalarmPrinter() {
        florian10Fetcher = new Florian10Fetcher();
    }

    public void run() {
        String[] urlToFetch = new String[5];
        urlToFetch[0] = "https://infoscreen.florian10.info/ows/infoscreen/einsatz.ashx";
        urlToFetch[1] = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=1";
        urlToFetch[2] = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=2";
        urlToFetch[3] = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=3";
        urlToFetch[4] = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=4";

        JSONObject florian10Data = florian10Fetcher.fetchFlorian10Data(urlToFetch[4]);

        if (florian10Data == null) {
            System.out.println("Fetching Data failed....");
        } else {
            System.out.println(florian10Data.toString(2));

            if (florian10Data.has("error")) {
                System.out.println("Failed to fetch data:");
                System.out.println("");
                System.out.println(florian10Data.toString(2));
                System.out.println("");
            } else if (florian10Data.has("CurrentState")) {
                if (florian10Data.get("CurrentState") == "waiting" && florian10Data.has("Token")) {
                    System.out.println("Token muss erst freigeschalten werden:");
                    System.out.println("");
                    System.out.println(florian10Data.get("Token"));
                    System.out.println("");
                }
                if (florian10Data.get("CurrentState") == "data" && florian10Data.has("EinsatzData")) {
                    JSONArray einsatzData = florian10Data.getJSONArray("EinsatzData");
                    System.out.println("EinsatzDaten: ");
                    System.out.println(einsatzData.toString(2));
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
