package at.kolmann.java.FFalarmPrinter;

import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

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


            // Force to TLS1.2
            // currently (2020-07-25) TLS1.3 fails with florian10
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);
            SSLContext.setDefault(context);


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
            System.out.println("Florian10Fetcher: Malformed URL exception....");
            mue.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("Florian10Fetcher: IOException....");
            ioe.printStackTrace();
        } catch (KeyManagementException e) {
            System.out.println("Florian10Fetcher: KeyManagementException....");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Florian10Fetcher: NoSuchAlgorithmException....");
            e.printStackTrace();
        }

        return null;
    }
}
