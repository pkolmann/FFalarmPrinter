package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

public class FFalarmPrinter {
    private Florian10Fetcher florian10Fetcher;

    public FFalarmPrinter() {
        System.out.println("inside FFalarmPrinter");
        florian10Fetcher = new Florian10Fetcher();
    }

    public void run() {
        System.out.println("running....");

        String urlToFetch = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=4";
        urlToFetch = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=3";
        urlToFetch = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=2";
        urlToFetch = "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=1";

        // Echt-URL
        //urlToFetch = "https://infoscreen.florian10.info/ows/infoscreen/einsatz.ashx";

        JSONObject florian10Data = florian10Fetcher.fetchFlorian10Data(urlToFetch);

        if (florian10Data == null) {
            System.out.println("Fetching Data failed....");
        } else {
            System.out.println(florian10Data.toString(2));

            if (florian10Data.has("CurrentState")) {
                System.out.println(florian10Data.get("CurrentState"));
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
