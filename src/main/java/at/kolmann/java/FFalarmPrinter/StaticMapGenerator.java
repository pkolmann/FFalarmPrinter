package at.kolmann.java.FFalarmPrinter;

import de.westnordost.osmapi.map.data.Node;
import org.json.JSONObject;

import java.util.ArrayList;

public interface StaticMapGenerator {
    byte[] getMapsImage(
            JSONObject route,
            Double einsatzLng,
            Double einsatzLat,
            ArrayList<Node> hydrants
    );
}
