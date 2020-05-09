package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;

public class EinsatzPDF {
    public void saveEinsatzPDF(String fileName, DirectionsRoute route, ImageResult einsatzMap) {
        System.out.println(route.summary);
        System.out.println(route.copyrights);
        System.out.println(route.toString());

        for (DirectionsLeg routeLeg : route.legs) {
            System.out.println("Leg:");
            System.out.println(routeLeg.toString());
            System.out.println("Start: " + routeLeg.startAddress);
            System.out.println("End: " + routeLeg.endAddress);
            System.out.println("Distance: " + routeLeg.distance.humanReadable);

            if (routeLeg.duration != null) {
                System.out.println("duration: " + routeLeg.duration.humanReadable);
            }
            if (routeLeg.durationInTraffic != null) {
                System.out.println("durationInTraffic: " + routeLeg.durationInTraffic.humanReadable);
            }
            System.out.println("");
            System.out.println("Steps:");
            for (DirectionsStep step : routeLeg.steps) {
                System.out.println("    htmlInstructions: " + step.htmlInstructions);
                System.out.println("    distance: " + step.distance.humanReadable);
                System.out.println("    duration: " + step.duration.humanReadable);
                System.out.println("    travelMode: " + step.travelMode.toString());

                System.out.println("++++++");
            }

            System.out.println("######");
        }
        System.out.println("-----");
    }
}
