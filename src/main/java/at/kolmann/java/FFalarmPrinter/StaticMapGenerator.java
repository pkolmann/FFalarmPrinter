package at.kolmann.java.FFalarmPrinter;

import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;
import de.westnordost.osmapi.map.data.Node;

import java.util.ArrayList;

public interface StaticMapGenerator {
    byte[] getMapsImage(
            DirectionsRoute route,
            LatLng einsatzLatLng,
            ArrayList<Node> hydrants
    );
}
