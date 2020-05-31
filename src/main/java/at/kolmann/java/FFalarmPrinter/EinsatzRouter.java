package at.kolmann.java.FFalarmPrinter;

import com.google.maps.*;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import de.westnordost.osmapi.map.data.Node;

import java.io.IOException;
import java.util.ArrayList;

public class EinsatzRouter {
    private final Config config;
    private final GeoApiContext context;
    private final DirectionsApiRequest directionsRequest;
    private ImageResult mapsImage;
    private DirectionsResult result;
    private DirectionsRoute route;

    private String destination;
    private LatLng einsatzLatLng;

    public EinsatzRouter(Config config) {
        this.config = config;

        context = new GeoApiContext.Builder()
                .apiKey(config.getString("googleMapsApiKey"))
                .disableRetries()
                .build();

        directionsRequest = new DirectionsApiRequest(context);
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
            e.printStackTrace();
        }

        // only use route 0
        if (result.routes.length > 0) {
            route = result.routes[0];
            einsatzLatLng = result.routes[0].legs[result.routes[0].legs.length - 1].endLocation;

        } else {
            System.out.println("No GeoAPI Result!");
        }
    }

    private void processMapImage(ArrayList<Node> hydrants) {
        if (route != null) {
            try {
                StaticMapsRequest.Markers markerA = new StaticMapsRequest.Markers();
                markerA.addLocation(new LatLng(
                        config.getDouble("FeuerwehrhausLocationLat"),
                        config.getDouble("FeuerwehrhausLocationLon")
                ));
                markerA.label("A");

                StaticMapsRequest.Markers markerE = new StaticMapsRequest.Markers();
                markerE.addLocation(destination);
                markerE.label("E");

                int mapZoom = 13;
                if (route.legs[0].distance.inMeters < 5000) {
                    mapZoom = 15;
                } else if (route.legs[0].distance.inMeters < 20000) {
                    mapZoom = 14;
                }

                // add hydrants as markers...
                StaticMapsRequest.Markers hydrantMarkers = new StaticMapsRequest.Markers();
                for (Node hydrant : hydrants) {
                    if (hydrant.isDeleted()) {
                        continue;
                    }
                    hydrantMarkers.addLocation(new LatLng(hydrant.getPosition().getLatitude(), hydrant.getPosition().getLongitude()));
                    hydrantMarkers.label("H");
                    hydrantMarkers.size(StaticMapsRequest.Markers.MarkersSize.tiny);
                    hydrantMarkers.color("blue");
                }

                mapsImage = new StaticMapsRequest(context)
                        .path(route.overviewPolyline)
                        .center(destination)
                        .zoom(mapZoom)
                        .markers(markerA)
                        .markers(markerE)
                        .markers(hydrantMarkers)
                        .format(StaticMapsRequest.ImageFormat.png)
                        .maptype(StaticMapsRequest.StaticMapType.roadmap)
                        .language("de")
                        .size(new Size(1280, 960))
                        .await();
            } catch (Exception e) {
                e.printStackTrace();
                context.shutdown();
            }
        }
    }

    public ImageResult getMapsImage(ArrayList<Node> hydrants) {
        if (destination == null) {
            return null;
        }
        if (mapsImage == null) {
            processMapImage(hydrants);
        }
        return mapsImage;
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
