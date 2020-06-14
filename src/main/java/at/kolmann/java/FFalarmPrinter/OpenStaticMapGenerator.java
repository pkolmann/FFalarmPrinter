package at.kolmann.java.FFalarmPrinter;

import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;
import de.westnordost.osmapi.map.data.Node;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class OpenStaticMapGenerator implements StaticMapGenerator {
    private  Config config;
    private final GeoApiContext context;
    private final String[] tileServer = {
            "https://maps.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps1.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps2.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps3.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps4.wien.gv.at/basemap/geolandbasemap/normal/google3857/"
    };

    private byte[] mapsImage = null;

    public OpenStaticMapGenerator(
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
                int mapZoom = 13;
                if (route.legs[0].distance.inMeters < 5000) {
                    mapZoom = 16;
                } else if (route.legs[0].distance.inMeters < 10000) {
                    mapZoom = 15;
                } else if (route.legs[0].distance.inMeters < 20000) {
                    mapZoom = 14;
                }

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
                Graphics2D baseMapGraphics = (Graphics2D) baseMapBuffer.getGraphics();
                int xOffset = 0;
                int yOffset = 0;
                int tileServerId = 0;
                for (int x = xtile - 2; x <= xtile + 2; x++) {
                    for (int y = ytile - 2; y <= ytile + 2; y++) {

                        String urlStr = tileServer[tileServerId] +
                                mapZoom +
                                "/" +
                                y +
                                "/" +
                                x +
                                ".png";
                        URL imageUrl =  new URL(urlStr);
                        tileServerId++;
                        if (tileServerId >= tileServer.length) tileServerId = 0;

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

                double north = tile2lat(ytile - 2, mapZoom);
                double south = tile2lat(ytile + 3, mapZoom);
                double west = tile2lon(xtile - 2, mapZoom);
                double east = tile2lon(xtile + 3, mapZoom);

                double xPixel = (north - south) / (256 * 5);
                double yPixel = (east - west) / (256 * 5);

                int lastX = -1;
                int lastY = -1;
                for (LatLng routePoint: route.overviewPolyline.decodePath()) {
                    if (routePoint.lat > north || routePoint.lat < south) continue;
                    if (routePoint.lng > east || routePoint.lng < west) continue;

                    int pointX = coord2pixel(routePoint.lng - west, yPixel);
                    int pointY = coord2pixel(north - routePoint.lat, xPixel);
                    if (lastX < 0 || lastY < 0) {
                        lastX = pointX;
                        lastY = pointY;
                        continue;
                    }
                    baseMapGraphics.setColor(Color.BLUE);
                    baseMapGraphics.setStroke(new BasicStroke(5));
                    baseMapGraphics.drawLine(lastX, lastY, pointX, pointY);
                    lastX = pointX;
                    lastY = pointY;
                }

                // Feuerwehrhaus im Image
                if (
                        config.getDouble("FeuerwehrhausLocationLat") < north &&
                        config.getDouble("FeuerwehrhausLocationLat") > south &&
                        config.getDouble("FeuerwehrhausLocationLon") > west &&
                        config.getDouble("FeuerwehrhausLocationLon") < east
                ) {
                    BufferedImage greenDot = mapMarker(Color.GREEN);
                    int FFx = coord2pixel(config.getDouble("FeuerwehrhausLocationLon") - west, yPixel);
                    int FFy = coord2pixel(north - config.getDouble("FeuerwehrhausLocationLat"), xPixel);
                    FFx = FFx - (int)Math.floor((double)greenDot.getWidth() / 2);
                    FFy = FFy - greenDot.getHeight();
                    baseMapGraphics.drawImage(greenDot, FFx, FFy, null);
                }

                // Einsatzort im Image
                int einsatzX = coord2pixel(einsatzLatLng.lng - west, yPixel);
                int einsatzY = coord2pixel(north - einsatzLatLng.lat,  xPixel);

                BufferedImage redDot = mapMarker(Color.RED);
                einsatzX = einsatzX - (int)Math.floor((double)redDot.getWidth() / 2);
                einsatzY = einsatzY - redDot.getHeight();
                baseMapGraphics.drawImage(redDot, einsatzX, einsatzY, null);


                // add hydrants as markers...
                BufferedImage blueDot = mapMarkerSmall(Color.BLUE);
                for (Node hydrant : hydrants) {
                    if (hydrant.isDeleted()) {
                        continue;
                    }
                    int hydrantX = coord2pixel(hydrant.getPosition().getLongitude() - west, yPixel);
                    int hydrantY = coord2pixel(north - hydrant.getPosition().getLatitude(), xPixel);
                    hydrantX = hydrantX - (int)Math.floor((double)blueDot.getWidth() / 2);
                    hydrantY = hydrantY - blueDot.getHeight();
                    baseMapGraphics.drawImage(blueDot, hydrantX, hydrantY, null);
                }

                // Cut image to size with EinsatzLocation in the middle and Save Image
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(baseMapBuffer.getSubimage(einsatzX - 320, einsatzY - 320, 640, 640),
                        "png", outputStream);
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

    private double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private int coord2pixel(double coord, double pixelDivisor) {
        return (int) Math.floor((coord) / pixelDivisor);
    }

    private BufferedImage mapMarker(Color color) {
        BufferedImage dotImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D dotImageGraphics = (Graphics2D) dotImage.getGraphics();
        dotImageGraphics.setBackground(new Color(1f, 0f, 0f, 0f));
        dotImageGraphics.setColor(color);
        dotImageGraphics.fillOval(4, 0, 23, 23);
        dotImageGraphics.setColor(Color.BLACK);
        dotImageGraphics.setStroke(new BasicStroke(1));
        dotImageGraphics.drawOval(4, 0, 23, 23);
        dotImageGraphics.setColor(color);
        dotImageGraphics.setStroke(new BasicStroke(4));
        dotImageGraphics.fillRect(14, 23, 3, 8);
        dotImageGraphics.setColor(Color.BLACK);
        dotImageGraphics.setStroke(new BasicStroke(1));
        dotImageGraphics.drawRect(14, 23, 3, 8);
        dotImageGraphics.setColor(color);
        dotImageGraphics.fillRect(15, 22, 2, 4);
        dotImageGraphics.dispose();
        return dotImage;
    }

    private BufferedImage mapMarkerSmall(Color color) {
        BufferedImage dotImage = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D dotImageGraphics = (Graphics2D) dotImage.getGraphics();
        dotImageGraphics.setBackground(new Color(1f, 0f, 0f, 0f));
        dotImageGraphics.setColor(color);
        dotImageGraphics.fillOval(3, 0, 18, 18);
        dotImageGraphics.setColor(Color.BLACK);
        dotImageGraphics.setStroke(new BasicStroke(1));
        dotImageGraphics.drawOval(3, 0, 18, 18);
        dotImageGraphics.setColor(color);
        dotImageGraphics.setStroke(new BasicStroke(3));
        dotImageGraphics.fillRect(11, 18, 2, 5);
        dotImageGraphics.setColor(Color.BLACK);
        dotImageGraphics.setStroke(new BasicStroke(1));
        dotImageGraphics.drawRect(11, 18, 2, 5);
        dotImageGraphics.setColor(color);
        dotImageGraphics.fillRect(12, 16, 1, 3);
        dotImageGraphics.dispose();
        return dotImage;
    }

}
