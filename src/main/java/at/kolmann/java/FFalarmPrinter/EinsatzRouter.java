package at.kolmann.java.FFalarmPrinter;

import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import de.westnordost.osmapi.map.data.Node;

import java.io.IOException;
import java.util.ArrayList;

public class EinsatzRouter {
    private final Config config;
    private final GeoApiContext context;
    private final DirectionsApiRequest directionsRequest;
    private byte[] mapsImage;
    private DirectionsResult result;
    private DirectionsRoute route = null;

    private String destination;
    private LatLng einsatzLatLng = null;

    private StaticMapGenerator staticMapGenerator;

    public EinsatzRouter(Config config) throws IOException {
        this.config = config;

        String googleApiKey = config.getString("googleMapsApiKeyWeb");
        if (config.has("googleMapsApiKeyStaticMap") && config.getString("googleMapsApiKeyStaticMap") != null) {
            googleApiKey = config.getString("googleMapsApiKeyStaticMap");
        }

        if (googleApiKey == null) {
            throw new IOException("No Google API Key found");
        }

        context = new GeoApiContext.Builder()
                .apiKey(googleApiKey)
                .disableRetries()
                .build();

        directionsRequest = new DirectionsApiRequest(context);

        if (config.has("googleMapsApiKeyStaticMap") && config.getString("googleMapsApiKeyStaticMap") != null) {
            staticMapGenerator = new GoogleStaticMapGenerator(config, context);
        } else {
            staticMapGenerator = new OpenStaticMapGenerator(config, context);
        }
    }

    public void shutdown() {
        context.shutdown();
    }

    private void processRoute() {
        try {
            if (config.getString("FeuerwehrhausLocationWaypoint") != null) {
                result = directionsRequest
                        .origin(
                                new LatLng(
                                        config.getDouble("FeuerwehrhausLocationLat"),
                                        config.getDouble("FeuerwehrhausLocationLon")
                                )
                        )
                        .waypoints(
                                new DirectionsApiRequest.Waypoint(
                                        config.getString("FeuerwehrhausLocationWaypoint"),
                                        false
                                )
                        )
                        .destination(destination)
                        .mode(TravelMode.DRIVING)
                        .units(Unit.METRIC)
                        .region("at")
                        .language("de-AT")
                        .await();
            } else {
                result = directionsRequest
                        .origin(
                                new LatLng(
                                        config.getDouble("FeuerwehrhausLocationLat"),
                                        config.getDouble("FeuerwehrhausLocationLon")
                                )
                        )
                        .destination(destination)
                        .mode(TravelMode.DRIVING)
                        .units(Unit.METRIC)
                        .region("at")
                        .language("de-AT")
                        .await();
            }
        } catch (ApiException | IOException | InterruptedException e) {
            System.out.println("Failed to get Route in EinsatzRouter.java:");
            e.printStackTrace();
            System.out.println("-----");
        }

        // only use route 0
        if (result != null && result.routes.length > 0) {
            route = result.routes[0];
            einsatzLatLng = result.routes[0].legs[result.routes[0].legs.length - 1].endLocation;

        } else {
            System.out.println("No GeoAPI Result!");
        }
    }


    public byte[] getMapsImage(ArrayList<Node> hydrants) {
        if (destination == null) {
            return null;
        }
        return staticMapGenerator.getMapsImage(route, einsatzLatLng, hydrants);
    }

    public DirectionsRoute getRoute(String destination) {
        if (destination == null) {
            return null;
        }

        this.destination = destination;
        if (route == null) {
            processRoute();
        }
        return route;
    }

    public LatLng getEinsatzLatLng() {
        return einsatzLatLng;
    }

}
