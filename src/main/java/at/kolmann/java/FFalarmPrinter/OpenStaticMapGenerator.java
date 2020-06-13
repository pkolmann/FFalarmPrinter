package at.kolmann.java.FFalarmPrinter;

import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;
import de.westnordost.osmapi.map.data.Node;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class OpenStaticMapGenerator implements StaticMapGenerator{
    private final GeoApiContext context;

    private byte[] mapsImage = null;

    public OpenStaticMapGenerator(
            Config config,
            GeoApiContext context
    ) {
        this.context = context;
    }

    private void processMapImage(
            DirectionsRoute route,
            LatLng einsatzLatLng,
            ArrayList<Node> hydrants
    ) {
        if (route != null) {
            try {
//                StaticMapsRequest.Markers markerA = new StaticMapsRequest.Markers();
//                markerA.addLocation(new LatLng(
//                        config.getDouble("FeuerwehrhausLocationLat"),
//                        config.getDouble("FeuerwehrhausLocationLon")
//                ));
//                markerA.label("A");
//
//                StaticMapsRequest.Markers markerE = new StaticMapsRequest.Markers();
//                markerE.addLocation(einsatzLatLng);
//                markerE.label("E");

                int mapZoom = 13;
                if (route.legs[0].distance.inMeters < 5000) {
                    mapZoom = 15;
                } else if (route.legs[0].distance.inMeters < 20000) {
                    mapZoom = 14;
                }

                System.out.println("EinsatzLoc: " + einsatzLatLng.lat + ", " + einsatzLatLng.lng);

                // OSM / Basemap tiles
                int xtile = (int)Math.floor( (einsatzLatLng.lng + 180) / 360 * (1<<mapZoom) ) ;
                int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(einsatzLatLng.lat)) + 1 /
                        Math.cos(Math.toRadians(einsatzLatLng.lat))) / Math.PI) / 2 * (1<<mapZoom) ) ;
                if (xtile < 0)
                    xtile=0;
                if (xtile >= (1<<mapZoom))
                    xtile=((1<<mapZoom)-1);
                if (ytile < 0)
                    ytile=0;
                if (ytile >= (1<<mapZoom))
                    ytile=((1<<mapZoom)-1);

                BufferedImage baseMapBuffer = new BufferedImage(256 * 5, 256 * 5, BufferedImage.TYPE_INT_RGB);
                Graphics baseMapGraphics = baseMapBuffer.getGraphics();
                int xOffset = 0;
                int yOffset = 0;
                for (int x = xtile - 2; x <= xtile + 2; x++) {
                    for (int y = ytile - 2; y <= ytile + 2; y++) {

                        String urlStr = "https://maps1.wien.gv.at/basemap/geolandbasemap/normal/google3857/" +
                                mapZoom +
                                "/" +
                                y +
                                "/" +
                                x +
                                ".png";
                        URL imageUrl =  new URL(urlStr);
                        try {
                            Image image = ImageIO.read(imageUrl);
                            baseMapGraphics.drawImage(image, xOffset, yOffset, null);
                            yOffset += 256;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    yOffset = 0;
                    xOffset += 256;
                }

                double north = Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * (ytile - 2)) / Math.pow(2.0, mapZoom))));
                double south = Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * (ytile + 3)) / Math.pow(2.0, mapZoom))));
                double west = (xtile - 2) / Math.pow(2.0, mapZoom) * 360.0 - 180;
                double east = (xtile + 3) / Math.pow(2.0, mapZoom) * 360.0 - 180;
                System.out.println("N: " + north + " S: " + south + " W: " + west + " E: " + east);

                double xPixel = (north - south) / (256 * 5);
                double yPixel = (east - west) / (256 * 5);

                System.out.println("xPixel: " + xPixel + ", yPixel: " + yPixel);


                for (LatLng routePoint: route.overviewPolyline.decodePath()) {
                    if (routePoint.lat > north || routePoint.lat < south) continue;
                    if (routePoint.lng > east || routePoint.lng < west) continue;
                    System.out.println("routePoint: " + routePoint.lat + "," + routePoint.lng);

                    int pointX = (int)Math.floor((routePoint.lng - west) / yPixel);
                    int pointY = (int)Math.floor((north - routePoint.lat) /  xPixel);
                    baseMapGraphics.setColor(Color.BLUE);
                    baseMapGraphics.fillOval(pointX, pointY, 10, 10);
                }

                // Einsatzort im Image
                int einsatzX = (int)Math.floor((einsatzLatLng.lng - west) / yPixel);
                int einsatzY = (int)Math.floor((north - einsatzLatLng.lat) /  xPixel);
                System.out.println("Einsatz: " + einsatzX + " , " + einsatzY);

                BufferedImage redDot = ImageIO.read(new File(System.getProperty("user.dir") + File.separator + "red-dot.png"));
                einsatzX = einsatzX - (int)Math.floor((double)redDot.getWidth() / 2);
                einsatzY = einsatzY - redDot.getHeight();
                baseMapGraphics.drawImage(redDot, einsatzX, einsatzY, null);



//                // add hydrants as markers...
//                StaticMapsRequest.Markers hydrantMarkers = new StaticMapsRequest.Markers();
//                for (Node hydrant : hydrants) {
//                    if (hydrant.isDeleted()) {
//                        continue;
//                    }
//                    hydrantMarkers.addLocation(new LatLng(hydrant.getPosition().getLatitude(), hydrant.getPosition().getLongitude()));
//                    hydrantMarkers.label("H");
//                    hydrantMarkers.size(StaticMapsRequest.Markers.MarkersSize.tiny);
//                    hydrantMarkers.color("blue");
//                }

                // Save Image
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(baseMapBuffer, "png", outputStream);
                mapsImage = outputStream.toByteArray();

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
