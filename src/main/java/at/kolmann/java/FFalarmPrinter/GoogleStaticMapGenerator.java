package at.kolmann.java.FFalarmPrinter;

import com.google.maps.GeoApiContext;
import com.google.maps.StaticMapsRequest;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;
import com.google.maps.model.Size;
import de.westnordost.osmapi.map.data.Node;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class GoogleStaticMapGenerator implements StaticMapGenerator {
    private final Config config;
    private final GeoApiContext context;

    private byte[] mapsImage = null;

    public GoogleStaticMapGenerator(
            Config config,
            GeoApiContext context
    ) {
        this.config = config;
        this.context = context;
    }

    private void processMapImage(
            DirectionsRoute route,
            LatLng einsatzLatLng,
            ArrayList<Node> hydrants
    ) {
        if (route != null) {
            try {
                StaticMapsRequest.Markers markerA = new StaticMapsRequest.Markers();
                markerA.addLocation(new LatLng(
                        config.getDouble("FeuerwehrhausLocationLat"),
                        config.getDouble("FeuerwehrhausLocationLon")
                ));
                markerA.label("A");

                StaticMapsRequest.Markers markerE = new StaticMapsRequest.Markers();
                markerE.addLocation(einsatzLatLng);
                markerE.label("E");

                int mapZoom = 13;
                if (route.legs[0].distance.inMeters < 5000) {
                    mapZoom = 16;
                } else if (route.legs[0].distance.inMeters < 10000) {
                    mapZoom = 15;
                } else if (route.legs[0].distance.inMeters < 20000) {
                    mapZoom = 14;
                }

                // add hydrants as markers...
                StaticMapsRequest.Markers hydrantMarkers = new StaticMapsRequest.Markers();
                if (hydrants != null) {
                    for (Node hydrant : hydrants) {
                        if (hydrant.isDeleted()) {
                            continue;
                        }
                        hydrantMarkers.addLocation(new LatLng(hydrant.getPosition().getLatitude(), hydrant.getPosition().getLongitude()));
                        hydrantMarkers.label("H");
                        hydrantMarkers.size(StaticMapsRequest.Markers.MarkersSize.tiny);
                        hydrantMarkers.color("blue");
                    }
                }

                mapsImage = new StaticMapsRequest(context)
                        .path(route.overviewPolyline)
                        .center(einsatzLatLng)
                        .zoom(mapZoom)
                        .markers(markerA)
                        .markers(markerE)
                        .markers(hydrantMarkers)
                        .format(StaticMapsRequest.ImageFormat.png)
                        .maptype(StaticMapsRequest.StaticMapType.roadmap)
                        .language("de")
                        .size(new Size(1280, 960))
                        .await()
                        .imageData;
            } catch (Exception e) {
                e.printStackTrace();
                context.shutdown();
            }
        }
    }

    @Override
    public byte[] getMapsImage(
            DirectionsRoute route,
            LatLng einsatzLatLng,
            ArrayList<Node> hydrants
    ) {
        if (mapsImage == null) {
            processMapImage(route, einsatzLatLng, hydrants);
        }
        return mapsImage;
    }
}
