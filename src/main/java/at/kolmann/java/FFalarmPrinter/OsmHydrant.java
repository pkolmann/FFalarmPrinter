package at.kolmann.java.FFalarmPrinter;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.overpass.OverpassMapDataDao;

import java.util.ArrayList;

public class OsmHydrant {
    final MyMapDataHandler mapDataHandler = new MyMapDataHandler();

    public ArrayList<Node> getHydrants(GeoLocation[] einsatzBox) {
        OsmConnection connection = new OsmConnection("https://overpass-api.de/api/", "my user agent");
        OverpassMapDataDao overpass = new OverpassMapDataDao(connection);

        String query = "node[emergency=fire_hydrant](" +
                einsatzBox[0].getLatitudeInDegrees() + "," +
                einsatzBox[0].getLongitudeInDegrees() + "," +
                einsatzBox[1].getLatitudeInDegrees() + "," +
                einsatzBox[1].getLongitudeInDegrees() + ")" +
                ";out;";
        overpass.queryElements(query, mapDataHandler);

        return mapDataHandler.getNodes();
    }
}
