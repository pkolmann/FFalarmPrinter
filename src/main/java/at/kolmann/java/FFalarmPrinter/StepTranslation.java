package at.kolmann.java.FFalarmPrinter;

public class StepTranslation {
    private Double distance = 0.0;
    private Double duration = 0.0;
    private String text = "";

    public StepTranslation(Double distance, Double duration, String text) {
        this.distance = distance;
        this.duration = duration;
        this.text = text;
    }

    public Double getDistance() {
        return this.distance;
    }

    public Double getDuration() {
        return duration;
    }

    public String getText() {
        return text;
    }

    public String toString() {
        return "{\"distance\": " + this.distance + ", " +
                "\"duration\": " + this.duration + ", " +
                "\"text\": \"" + this.text + "\"}";
    }
}
