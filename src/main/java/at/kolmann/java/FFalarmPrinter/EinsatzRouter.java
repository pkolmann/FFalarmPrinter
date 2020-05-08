package at.kolmann.java.FFalarmPrinter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.*;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;

import java.io.FileOutputStream;
import java.io.IOException;

public class EinsatzRouter {
    private Config config;
    private GeoApiContext context;
    private GeocodingResult[] start = null;
    private DirectionsApiRequest directionsRequest;
    private ImageResult mapsImage;
    private DirectionsResult result;

    public EinsatzRouter(Config config) {
        this.config = config;

        context = new GeoApiContext.Builder()
                .apiKey(config.getString("googleMapsApiKey"))
                .disableRetries()
                .build();

        try {
            if (config.get("FeuerwehrhausLocationLat") == null ||
                    config.get("FeuerwehrhausLocationLon") == null) {
                System.out.println("Keine Feuerwehrhaus-Location angegeben!");
            }
            start = GeocodingApi.newRequest(context)
                    .latlng(
                            new LatLng(
                                    config.getDouble("FeuerwehrhausLocationLat"),
                                    config.getDouble("FeuerwehrhausLocationLon")
                            )
                    )
                    .await();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        directionsRequest = new DirectionsApiRequest(context);
    }

    public void shutdown() {
        context.shutdown();
    }

    public int getRoute(String destination) {
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
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (result != null) {
            try {
                System.out.println("Destination: " + destination);
                System.out.println("Routes: " + result.routes.length);
                System.out.println("WayPoints: " + result.geocodedWaypoints.length);
                System.out.println("");
                System.out.println("Routes: ");
                // only use route 0
                System.out.println("Routes: " + result.routes.length);
                if (result.routes.length > 0) {
                    DirectionsRoute route = result.routes[0];
                    System.out.println(route.summary);
                    System.out.println(route.copyrights);
                    System.out.println(route.toString());

                    StaticMapsRequest.Markers markerA = new StaticMapsRequest.Markers();
                    markerA.addLocation(new LatLng(
                            config.getDouble("FeuerwehrhausLocationLat"),
                            config.getDouble("FeuerwehrhausLocationLon")
                    ));
                    markerA.label("A");
                    
                    StaticMapsRequest.Markers markerE = new StaticMapsRequest.Markers();
                    markerE.addLocation(destination);
                    markerE.label("E");

                    mapsImage = new StaticMapsRequest(context)
                            .path(route.overviewPolyline)
                            .markers(markerA)
                            .markers(markerE)
                            .format(StaticMapsRequest.ImageFormat.png)
                            .maptype(StaticMapsRequest.StaticMapType.roadmap)
                            .language("de")
                            .size(new Size(1280, 960))
                            .await();

                    for (DirectionsLeg routeLeg : route.legs) {
                        System.out.println("Leg:");
                        System.out.println(routeLeg.toString());
                        System.out.println("Start: " + routeLeg.startAddress);
                        System.out.println("End: " + routeLeg.endAddress);
                        System.out.println("Distance: " + routeLeg.distance.humanReadable);

                        if (routeLeg.duration != null) {
                            System.out.println("duration: " + routeLeg.duration.humanReadable);
                        }
                        if (routeLeg.durationInTraffic != null) {
                            System.out.println("durationInTraffic: " + routeLeg.durationInTraffic.humanReadable);
                        }
                        System.out.println("");
                        System.out.println("Steps:");
                        for (DirectionsStep step : routeLeg.steps) {
                            System.out.println("    htmlInstructions: " + step.htmlInstructions);
                            System.out.println("    distance: " + step.distance.humanReadable);
                            System.out.println("    duration: " + step.duration.humanReadable);
                            System.out.println("    travelMode: " + step.travelMode.toString());

                            System.out.println("++++++");
                        }

                        System.out.println("######");
                    }
                    System.out.println("-----");
                }
                System.out.println("=======");

            } catch (Exception e) {
                e.printStackTrace();
                context.shutdown();
            }
        } else {
            System.out.println("No GeoAPI Result!");
        }


        return 0;
    }

    public ImageResult getMapsImage() {
        return mapsImage;
    }
    public DirectionsResult getMapsResult() {
        return result;
    }

}
