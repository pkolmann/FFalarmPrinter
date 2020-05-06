package at.kolmann.java.FFalarmPrinter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.*;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;

import java.io.FileOutputStream;
import java.io.IOException;

public class EinsatzRouter {
    private Config config = null;
    private GeoApiContext context = null;
    private GeocodingResult[] start = null;
    private DirectionsApiRequest directionsRequest = null;
    private ImageResult mapsImage = null;
    private DirectionsResult result = null;

    public EinsatzRouter(Config config) {
        this.config = config;

        context = new GeoApiContext.Builder()
                .apiKey((String) config.get("googleMapsApiKey"))
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
                                    (double) config.get("FeuerwehrhausLocationLat"),
                                    (double) config.get("FeuerwehrhausLocationLon")
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
            result = directionsRequest
                    .origin(
                            new LatLng(
                                    (double) config.get("FeuerwehrhausLocationLat"),
                                    (double) config.get("FeuerwehrhausLocationLon")
                            )
                    )
                    .destination(destination)
                    .mode(TravelMode.DRIVING)
                    .units(Unit.METRIC)
                    .region("at")
                    .language("de-AT")
                    .await();

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
                for (DirectionsRoute route : result.routes) {
                    System.out.println(route.summary);
                    System.out.println(route.copyrights);
                    System.out.println(route.toString());

                    mapsImage = new StaticMapsRequest(context)
                            .path(route.overviewPolyline)
                            .format(StaticMapsRequest.ImageFormat.png)
                            .maptype(StaticMapsRequest.StaticMapType.roadmap)
                            .language("de")
                            .size(new Size(640, 480))
                            .await();

                    try (FileOutputStream fos = new FileOutputStream("pathname")) {
                        fos.write(myByteArray);
                        //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
                    }

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
