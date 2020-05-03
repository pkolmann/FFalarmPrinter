package at.kolmann.java.FFalarmPrinter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.*;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;

import java.io.IOException;

public class EinsatzRouter {
    Config config;
    GeoApiContext context;
    GeocodingResult[] start;
    DirectionsApiRequest directionsRequest;

    public EinsatzRouter(Config config) {
        this.config = config;

        context = new GeoApiContext.Builder()
                .apiKey((String) config.get("googleMapsApiKey"))
                .disableRetries()
                .build();

        try {
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

    public int getRoute(String destination) {
        DirectionsResult result = null;
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
                    .await();
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Destination: " + destination);
        for (DirectionsRoute route : result.routes) {
            System.out.println(route.summary);
            System.out.println(route.copyrights);
            System.out.println(route.toString());
            System.out.println("=======");
        }

        for (GeocodedWaypoint wayPoint: result.geocodedWaypoints) {
            System.out.println("Waypoint:");
            System.out.println(wayPoint.placeId);
        }




        return 0;
    }
}
