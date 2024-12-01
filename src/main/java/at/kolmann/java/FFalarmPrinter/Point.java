package at.kolmann.java.FFalarmPrinter;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * https://github.com/scoutant/polyline-decoder/blob/master/src/main/java/org/scoutant/polyline/Point.java
 *
 * Simple geographical point represented by a couple of doubles. Google's GeoPoint is a couple of micro-degrees
 * represented by integers.
 */
public class Point implements Serializable {
    private static final long serialVersionUID = 1L;
    private final double lat;
    private final double lng;

    public Point(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    @Override
    public String toString() {
        return "(" + lat + ", " + lng + ")";
    }

    /**
     * Utility method to export coordinates for use with GeoJSON. This standard requires longitude first. See
     * http://geojson.org/geojson-spec.html#positions
     */
    public static String toGeoJSON(List<Point> points) {
        StringBuilder buff = new StringBuilder("[");
        Iterator<Point> itr = points.iterator();
        while (itr.hasNext()) {
            buff.append(toGeoJSON(itr.next()));
            if (itr.hasNext()) {
                buff.append(",");
            }
        }
        buff.append("]");
        return buff.toString();
    }

    public static String toGeoJSON(Point point) {
        return "[" + point.getLng() + "," + point.getLat() + "]";
    }

    /**
     * We consider that two point are equals if both latitude and longitude are "nearly" the same. With a precision of
     * 1e-3 degree
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) {
            return false;
        }
        Point that = (Point) o;
        if (Math.abs(that.getLat() - lat) > 0.001) {
            return false;
        }
        return Math.abs(that.getLng() - lng) <= 0.001;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.lat) ^ (Double.doubleToLongBits(this.lat) >>> 32));
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.lng) ^ (Double.doubleToLongBits(this.lng) >>> 32));
        return hash;
    }

    /**
     * Compute the distance between two points in meters. The formula is the haversine formula.
     * @param that the other point
     * @return the distance in meters
     */
    public double distanceTo(Point that) {
        double lat1 = Math.toRadians(this.lat);
        double lon1 = Math.toRadians(this.lng);
        double lat2 = Math.toRadians(that.lat);
        double lon2 = Math.toRadians(that.lng);
        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;
        double a = Math.pow(Math.sin(deltaLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371e3 * c;
    }
}

