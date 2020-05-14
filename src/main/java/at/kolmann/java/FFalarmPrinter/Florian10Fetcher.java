package at.kolmann.java.FFalarmPrinter;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;

public class Florian10Fetcher {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    public Florian10Fetcher(String cookieFile) {
        CookieManager cookieManager = new CookieManager(
                new PersistentCookieStore(
                        System.getProperty("user.dir") + File.separator,
                        cookieFile
                ),
                null);
        CookieHandler.setDefault(cookieManager);
    }

    public JSONObject fetchFlorian10Data(String urlToFetch) {
        try {
            URL url = new URL(urlToFetch);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                JSONObject error = new JSONObject();
                error.put("error", "HTTP Response: " + connection.getResponseCode());
                return error;
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder florian10Response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                florian10Response.append(inputLine);
            }

            JSONObject florian10Data = new JSONObject(florian10Response.toString());
            in.close();
            return florian10Data;

        } catch (MalformedURLException mue) {
            System.out.println("Malformed URL exception....");
            mue.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("IOException....");
            ioe.printStackTrace();
        }

        return null;
    }
}
