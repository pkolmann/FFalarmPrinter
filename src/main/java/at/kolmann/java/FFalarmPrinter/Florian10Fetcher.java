package at.kolmann.java.FFalarmPrinter;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

public class Florian10Fetcher {
    private final HttpClient httpClient;

    private final CookieManager cookieManager;
    public Florian10Fetcher(String cookieFile) {
        this.cookieManager = new CookieManager(
                new PersistentCookieStore(
                        System.getProperty("user.dir") + File.separator,
                        cookieFile
                ),
                CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(this.cookieManager);

        httpClient =  HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .cookieHandler(this.cookieManager)
                .build();
    }

    public JSONObject fetchFlorian10Data(String urlToFetch) {
        try {
            HttpURLConnection connection = getHttpURLConnection(urlToFetch);

            List<HttpCookie> cookies = this.cookieManager.getCookieStore().getCookies();
            Map<String, List<String>> headers = connection.getHeaderFields();
//            System.out.println("Connection Response: " + connection.getResponseCode());
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
        }

        return null;
    }

    private static HttpURLConnection getHttpURLConnection(String urlToFetch) throws IOException {
        URL url = new URL(urlToFetch);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
//            connection.setRequestProperty("Cache-Control", "no-cache");
//            connection.setRequestProperty("Pragma", "no-cache");
//            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
//            connection.setRequestProperty("Accept-Encoding", "gzip, deflate,br");
//            connection.setRequestProperty("Accept-Language", "de,en-US;q=0.7,en;q=0.3");
//            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:78.0) Gecko/20100101 Firefox/78.0");


        connection.setRequestMethod("GET");
        return connection;
    }
}
