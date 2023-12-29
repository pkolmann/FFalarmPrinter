package at.kolmann.java.FFalarmPrinter;

import de.westnordost.osmapi.map.data.Node;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class OpenStaticMapGenerator implements StaticMapGenerator {
    private final Config config;

    private final PolylineDecoder polylineDecoder = new PolylineDecoder();

    private final String[] tileServer = {
            "https://maps.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps1.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps2.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps3.wien.gv.at/basemap/geolandbasemap/normal/google3857/",
            "https://maps4.wien.gv.at/basemap/geolandbasemap/normal/google3857/"
    };

    static final private int NUMBER_OF_TILES = 5;
    static final private int TILE_PIXELS = 256;

    private byte[] mapsImage = null;

    public OpenStaticMapGenerator(Config config) {
        this.config = config;
    }

    private void processMapImage(
            JSONObject route,
            Double einsatzLng,
            Double einsatzLat,
            ArrayList<Node> hydrants
    ) {
        if (route != null) {
            try {
                int mapZoom = 13;

                long totalDistance = 0;
                if (route.has("distance")) {
                    totalDistance = route.getLong("distance");
                }

                if (totalDistance > 0 && totalDistance < 5000) {
                    mapZoom = 16;
                } else if (totalDistance < 10000) {
                    mapZoom = 15;
                } else if (totalDistance < 20000) {
                    mapZoom = 14;
                }

                // OSM / Basemap tiles
                int xtile = (int)Math.floor( (einsatzLng + 180) / 360 * (1<<mapZoom) ) ;
                int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(einsatzLat)) + 1 /
                        Math.cos(Math.toRadians(einsatzLat))) / Math.PI) / 2 * (1<<mapZoom) ) ;
                if (xtile < 0)
                    xtile=0;
                if (xtile >= (1<<mapZoom))
                    xtile=((1<<mapZoom)-1);
                if (ytile < 0)
                    ytile=0;
                if (ytile >= (1<<mapZoom))
                    ytile=((1<<mapZoom)-1);

                BufferedImage baseMapBuffer = new BufferedImage(TILE_PIXELS * NUMBER_OF_TILES,
                        TILE_PIXELS * NUMBER_OF_TILES, BufferedImage.TYPE_INT_RGB);
                Graphics2D baseMapGraphics = (Graphics2D) baseMapBuffer.getGraphics();
                int xOffset = 0;
                int yOffset = 0;
                int tileServerId = 0;
                int tileDiff = (int)Math.floor((NUMBER_OF_TILES - 1) / 2.0 );
                for (int x = xtile - tileDiff; x <= xtile + tileDiff; x++) {
                    for (int y = ytile - tileDiff; y <= ytile + tileDiff; y++) {

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
                            yOffset += TILE_PIXELS;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    yOffset = 0;
                    xOffset += TILE_PIXELS;
                }

                double north = tile2lat(ytile - tileDiff, mapZoom);
                double south = tile2lat(ytile + tileDiff + 1, mapZoom);
                double west = tile2lon(xtile - tileDiff, mapZoom);
                double east = tile2lon(xtile + tileDiff + 1, mapZoom);

                double xPixel = (north - south) / (TILE_PIXELS * NUMBER_OF_TILES);
                double yPixel = (east - west) / (TILE_PIXELS * NUMBER_OF_TILES);

                int lastX = -1;
                int lastY = -1;
                if (route.has("geometry")) {
                    for (Point routePoint: polylineDecoder.decode(route.getString("geometry"))) {
                        if (routePoint.getLat() > north || routePoint.getLat() < south) continue;
                        if (routePoint.getLng() > east || routePoint.getLng() < west) continue;

                        int pointX = coord2pixel(routePoint.getLng() - west, yPixel);
                        int pointY = coord2pixel(north - routePoint.getLat(), xPixel);
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
                int einsatzX = coord2pixel(einsatzLng - west, yPixel);
                int einsatzY = coord2pixel(north - einsatzLat,  xPixel);

                BufferedImage redDot = mapMarker(Color.RED);
                einsatzX = einsatzX - (int)Math.floor((double)redDot.getWidth() / 2);
                einsatzY = einsatzY - redDot.getHeight();
                baseMapGraphics.drawImage(redDot, einsatzX, einsatzY, null);


                // add hydrants as markers...
                BufferedImage blueDot = mapMarkerSmall(Color.BLUE);
                if (hydrants != null) {
                    System.out.println("Adding hydrants...");
                    for (Node hydrant : hydrants) {
                        if (hydrant.isDeleted()) {
                            continue;
                        }
                        int hydrantX = coord2pixel(hydrant.getPosition().getLongitude() - west, yPixel);
                        int hydrantY = coord2pixel(north - hydrant.getPosition().getLatitude(), xPixel);
                        hydrantX = hydrantX - (int) Math.floor((double) blueDot.getWidth() / 2);
                        hydrantY = hydrantY - blueDot.getHeight();
                        baseMapGraphics.drawImage(blueDot, hydrantX, hydrantY, null);
                    }
                } else {
                    System.out.println("No Hydrants found!");
                }

                // Add scale to bottom
                double middleLat = south + ((north - south) / 2);
                // Max 100 px scale
                double distanceWest = tile2lon(xtile, mapZoom);
                double distanceEast = distanceWest + ((tile2lon(xtile + 1, mapZoom) - distanceWest) * 100.0 / TILE_PIXELS);
                double distance = haversine(middleLat, distanceWest, middleLat, distanceEast);
                long roundDistance = getRoundNum((long) distance);
                int scalePixel = (int) Math.round(100.0 * (roundDistance / distance));

                // scalePixel Pixel equals roundDistance Meters in reality
                baseMapGraphics.setStroke(new BasicStroke(1));
                baseMapGraphics.setColor(Color.BLACK);
                baseMapGraphics.fillRect(einsatzX - 280, einsatzY + 280, scalePixel, 5);
                String distanceString = roundDistance + "m";
                if (roundDistance > 1000) {
                    distanceString = (roundDistance / 1000) + "km";
                }
                int width = baseMapGraphics.getFontMetrics().stringWidth(distanceString);
                baseMapGraphics.drawString(distanceString, einsatzX - 280 + scalePixel / 2 - width / 2, einsatzY + 300);


                // Cut image to size with EinsatzLocation in the middle and Save Image
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(baseMapBuffer.getSubimage(einsatzX - 320, einsatzY - 320, 640, 640),
                        "png", outputStream);
                mapsImage = outputStream.toByteArray();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] getMapsImage(
            JSONObject route,
            Double einsatzLng,
            Double einsatzLat,
            ArrayList<Node> hydrants
    ) {
        if (mapsImage == null) {
            processMapImage(route, einsatzLng, einsatzLat, hydrants);
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

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        // Mean Earth Radius, as recommended for use by
        // the International Union of Geodesy and Geophysics,
        // see http://rosettacode.org/wiki/Haversine_formula
        double earthRadius = 6371000.0; // in meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return earthRadius * c;
    }

    private long getRoundNum(long num) {
        // Thanks to https://github.com/Leaflet/Leaflet/blob/master/src/control/Control.Scale.js#L114
        long pow10 = (long) Math.pow(10, ((long)Math.floor(num) + "").length() -1);
        long d = num / pow10;

        d = d >= 10 ? 10 :
            d >= 5 ? 5 :
            d >= 3 ? 3 :
            d >= 2 ? 2 : 1;

        return pow10 * d;
    }

}
