package at.kolmann.java.FFalarmPrinter;

import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.handler.MapDataHandler;

import java.util.ArrayList;

public class MyMapDataHandler implements MapDataHandler {
    private final ArrayList<BoundingBox> boundingBoxes = new ArrayList<>();
    private final ArrayList<Node> nodes = new ArrayList<>();
    private final ArrayList<Way> ways = new ArrayList<>();
    private final ArrayList<Relation> relations = new ArrayList<>();

    @Override
    public void handle(BoundingBox boundingBox) {
        boundingBoxes.add(boundingBox);
    }

    @Override
    public void handle(Node node) {
        nodes.add(node);
    }

    @Override
    public void handle(Way way) {
        ways.add(way);
    }

    @Override
    public void handle(Relation relation) {
        relations.add(relation);
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public ArrayList<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public ArrayList<Way> getWays() {
        return ways;
    }

    public ArrayList<Relation> getRelations() {
        return relations;
    }
}
