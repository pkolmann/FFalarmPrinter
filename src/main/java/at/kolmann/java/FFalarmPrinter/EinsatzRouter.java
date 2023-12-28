package at.kolmann.java.FFalarmPrinter;

import de.westnordost.osmapi.map.data.Node;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

public class EinsatzRouter {
    private final Config config;
    private byte[] mapsImage;
    private JSONObject result = null;
    private JSONObject route = null;

    private Double einsatzLat = null;
    private Double einsatzLng = null;

    private StaticMapGenerator staticMapGenerator;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public EinsatzRouter(Config config) throws IOException {
        this.config = config;
        staticMapGenerator = new OpenStaticMapGenerator(config);
    }

    public void shutdown() {}

    private void processRoute() throws IOException {
        try {
            if (config.getString("osrmBaseURL") == null) {
                throw new IOException("osrmBaseURL not defined in config file!");
            }

            if (this.einsatzLng == null || this.einsatzLat == null) {
                throw new IOException("Einsatzrouter: EinsatzLng or EinsatzLat IS NULL!");
            }

            // Build OSRM URI:
            // https://osrm.ff-irgendwo.at/route/v1/driving/16.18417,47.71375;16.18576,47.71566;16.192861,47.696053?steps=true&overview=full
            StringBuilder uri = new StringBuilder();
            uri.append(config.getString("osrmBaseURL"));
            uri.append("/route/v1/driving/");
            uri.append(config.getDouble("FeuerwehrhausLocationLon"));
            uri.append(",");
            uri.append(config.getDouble("FeuerwehrhausLocationLat"));
            uri.append(";");
            if (
                    config.has("FeuerwehrhausLocationWaypointLon")
                    && config.has("FeuerwehrhausLocationWaypointLat")
                    && config.get("FeuerwehrhausLocationWaypointLon") != null
                    && config.get("FeuerwehrhausLocationWaypointLat") != null
            ) {
                uri.append(config.getDouble("FeuerwehrhausLocationWaypointLon"));
                uri.append(",");
                uri.append(config.getDouble("FeuerwehrhausLocationWaypointLat"));
                uri.append(";");
            }
            uri.append(this.einsatzLng);
            uri.append(",");
            uri.append(this.einsatzLat);
            uri.append("?steps=true&overview=full");

            System.out.println(uri.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(uri.toString()))
                    .setHeader("User-Agent", "FF Alarm Printer")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch OSRM data: " + response.statusCode() + "\n\n" + response.body());
            }
            // print response body
            result = new JSONObject(response.body());

        } catch (IOException | InterruptedException e) {
            System.out.println("Failed to get Route in EinsatzRouter.java:");
            e.printStackTrace();
            System.out.println("-----");
        }

        // only use route 0
        if (result == null) {
            throw new IOException("Failed to get RouteJSON!");
        }
        if (!result.has("routes")) {
            throw new IOException("RouteJSON has no 'routes' key!");
        }

        if (result.getJSONArray("routes").length() < 1) {
            throw new IOException("RouteJSON has less then 1 routes!");
        }

        route = result.getJSONArray("routes").getJSONObject(0);
    }


    public byte[] getMapsImage(ArrayList<Node> hydrants) {
        if (einsatzLng == null || einsatzLat == null) {
            System.out.println("EinsatzRouter - getMapsImage - einsatzLatLng == null");
            return null;
        }
        return staticMapGenerator.getMapsImage(route, einsatzLng, einsatzLat, hydrants);
    }

    public JSONObject getRoute(Double einsatzLng, Double einsatzLat) {
        if (einsatzLng == null || einsatzLat == null) {
            return null;
        }

        this.einsatzLng = einsatzLng;
        this.einsatzLat = einsatzLat;
        if (route == null) {
            try {
                processRoute();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        return route;
    }

    public JSONObject getResult() {
        return result;
    }

    public Point getEinsatzLatLng() {
        if (einsatzLat == null || einsatzLng == null) {
            return  null;
        }
        return new Point(einsatzLat, einsatzLng);
    }
}
