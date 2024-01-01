package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

/*
 * Main ideas taken from
 * https://github.com/Project-OSRM/osrm-text-instructions.java/blob/5756f25c862d64ba61debb004b9c409285869111/osrm-text-instructions/libjava-osrm-instructions/src/main/java/org/project_osrm/instructions/TextInstructions.java
 */

public class OsrmTextInstructions {
    private JSONObject langObject = new JSONObject();

    public OsrmTextInstructions() {
        loadLangFile(null);
    }

    public OsrmTextInstructions(String languageFileStr) {
        loadLangFile(languageFileStr);
    }

    private void loadLangFile(String languageFileStr) {
        // Load the resource
        File languageFile = null;

        if (languageFileStr != null) {
            languageFile = new File(languageFileStr);
            if (!languageFile.isAbsolute()) {
                languageFile = new File(System.getProperty("user.dir") + File.separator + languageFileStr);
            }
        }
        if (languageFileStr == null || !languageFile.exists()) {
            languageFile = new File(System.getProperty("user.dir") + File.separator + "osrm_de.json");
        }

        if (languageFile.exists()) {
            StringBuilder langString = Tools.readFile(languageFile);

            JSONObject loadedObject = new JSONObject(langString.toString());
            String mainLangKey = "v5";
            if (loadedObject.has(mainLangKey)) {
                this.langObject = loadedObject.getJSONObject(mainLangKey);
            }
        } else {
            System.out.println("No languageFile found....");
            System.out.println("Loaded default values...");
        }
    }

    public static String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Transform numbers to their translated ordinalized value
     *
     * @param number value
     * @return translated ordinalized value
     */
    public String ordinalize(Integer number) {
        try {
            return this.langObject.getJSONObject("constants").getJSONObject("ordinalize").getString(String.valueOf(number));
        } catch (Exception exception) {
            return number.toString() + ".";
        }
    }

    /**
     * Transform degrees to their translated compass direction
     *
     * @param degree value
     * @return translated compass direction
     */
    public String directionFromDegree(Double degree) {
        if (degree == null) {
            // step had no bearing_after degree, ignoring
            return "";
        } else if (degree >= 0 && degree <= 20) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("north");
        } else if (degree > 20 && degree < 70) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("northeast");
        } else if (degree >= 70 && degree <= 110) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("east");
        } else if (degree > 110 && degree < 160) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("southeast");
        } else if (degree >= 160 && degree <= 200) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("south");
        } else if (degree > 200 && degree < 250) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("southwest");
        } else if (degree >= 250 && degree <= 290) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("west");
        } else if (degree > 290 && degree < 340) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("northwest");
        } else if (degree >= 340 && degree <= 360) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction").getString("north");
        } else {
            throw new RuntimeException("Degree is invalid: " + degree);
        }
    }

    /**
     * Reduce any lane combination down to a contracted lane diagram
     *
     * @param step a route step
     */
    public String laneConfig(JSONObject step) {
        if (
            !step.has("intersections")
            || step.getJSONArray("intersections").isEmpty()
            || !((JSONObject) step.getJSONArray("intersections").get(0)).has("lanes")
            || ((JSONObject) step.getJSONArray("intersections").get(0)).getJSONArray("lanes").isEmpty()
        ) {
            throw new RuntimeException("No lanes object");
        }

        StringBuilder config = new StringBuilder();
        Boolean currentLaneValidity = null;
        JSONArray lanes = ((JSONObject) step.getJSONArray("intersections").get(0)).getJSONArray("lanes");
        for (int i = 0; i < lanes.length(); i++) {
            JSONObject lane = (JSONObject) lanes.get(i);
            if (
                    currentLaneValidity == null
                    || (lane.has("valid")
                        && currentLaneValidity != lane.getBoolean("valid")
                    )
            ) {
                if (lane.getBoolean("valid")) {
                    config.append("o");
                } else {
                    config.append("x");
                }
                currentLaneValidity = lane.getBoolean("valid");
            }
        }

        return config.toString();
    }

    public StepTranslation compile(JSONObject step) {
        if (!step.has("maneuver")) {
            throw new RuntimeException("No step maneuver provided.");
        }
        JSONObject maneuver = step.getJSONObject("maneuver");
        if (!maneuver.has("type")) {
            throw new RuntimeException("No maneuver type provided.");
        }

        String type = maneuver.getString("type");
        String modifier = "";
        if (maneuver.has("modifier")) {
            modifier = maneuver.getString("modifier");
        }
        String mode = null;
        if (maneuver.has("mode")) {
            mode = maneuver.getString("mode");
        }

        if (type.isEmpty()) {
            throw new RuntimeException("Missing step maneuver type.");
        }

        if (!type.equals("depart") && !type.equals("arrive") && modifier.isEmpty()) {
            throw new RuntimeException("Missing step maneuver modifier.");
        }

        if (!this.langObject.has(type)) {
            // Log for debugging
            System.out.println("Encountered unknown instruction type: " + type);

            // OSRM specification assumes turn types can be added without
            // major version changes. Unknown types are to be treated as
            // type `turn` by clients
            type = "turn";
        }

        // Use special instructions if available, otherwise `defaultinstruction`
        JSONObject instructionObject;
        JSONObject modeValue = null;
        if (this.langObject.getJSONObject("modes").has(mode)) {
            modeValue = this.langObject.getJSONObject("modes").getJSONObject(mode);
        }
        if (modeValue != null) {
            instructionObject = modeValue;
        } else {
            JSONObject modifierValue = null;
            if (this.langObject.getJSONObject(type).has(modifier)) {
                modifierValue = this.langObject.getJSONObject(type).getJSONObject(modifier);
            }
            instructionObject = modifierValue == null ? this.langObject.getJSONObject(type).getJSONObject("default") : modifierValue;
        }

        // Special case handling
        String laneInstruction = null;
        switch (type) {
            case "use lane":
                String laneConfig = laneConfig(step);
                if (this.langObject.has("constants") && this.langObject.getJSONObject("constants").has("lanes") && this.langObject.getJSONObject("constants").getJSONObject("lanes").has(laneConfig)) {
                    laneInstruction = this.langObject.getJSONObject("constants").getJSONObject("lanes").getString(laneConfig);
                }
                if (laneInstruction == null) {
                    // If the lane combination is not found, default to continue straight
                    instructionObject = this.langObject.getJSONObject("use lane").getJSONObject("no_lanes");
                }
                break;
            case "rotary":
            case "roundabout":
                if (
                        this.langObject.has("name")
                                && !this.langObject.getString("name").isEmpty()
                                && this.langObject.has("maneuver")
                                && this.langObject.getJSONObject("maneuver").has("exit")
                                && instructionObject.has("name_exit")
//                    && this.langObject.getJSONObject("maneuver").getInt("exit")
//                        !TextUtils.isEmpty(step.getRotaryName()) && step.getManeuver().getExit() != null && instructionObject.getAsJsonObject("name_exit") != null
                ) {
                    instructionObject = instructionObject.getJSONObject("name_exit");
                } else if (
                        this.langObject.has("name")
                                && instructionObject.has("name")
//                    step.getRotaryName() != null && instructionObject.getAsJsonObject("name") != null
                ) {
                    instructionObject = instructionObject.getJSONObject("name");
                } else if (
                        this.langObject.has("maneuver")
                                && this.langObject.getJSONObject("maneuver").has("exit")
                                && instructionObject.has("exit")
//                    step.getManeuver().getExit() != null && instructionObject.getAsJsonObject("exit") != null
                ) {
                    instructionObject = instructionObject.getJSONObject("exit");
                } else {
                    instructionObject = instructionObject.getJSONObject("default");
                }
                break;
            default:
                // NOOP, since no special logic for that type
        }

        // Decide way_name with special handling for name and ref
        String wayName;
        String name = "";
        if (step.has("name") && !step.getString("name").isEmpty()) {
            name = step.getString("name");
        }
        String ref = "";
        if (step.has("ref") && !step.getString("ref").isEmpty()) {
            ref = step.getString("ref").split(";")[0];
        }

        // Remove hacks from Mapbox Directions mixing ref into name
        if (step.has("ref") && name.equals(step.getString("ref"))) {
            // if both are the same we assume that there used to be an empty name, with the ref being filled in for it
            // we only need to retain the ref then
            name = "";
        }
        if (step.has("ref")) {
            name = name.replace(" (" + step.getString("ref") + ")", "");
        }

        if (!name.isEmpty() && !ref.isEmpty() && !name.equals(ref)) {
            wayName = name + " (" + ref + ")";
        } else if (name.isEmpty() && !ref.isEmpty()) {
            wayName = ref;
        } else {
            wayName = name;
        }

        // Decide which instruction string to use
        // Destination takes precedence over name
        String instruction;
        if (
            step.has("destinations")
            && !step.getString("destinations").isEmpty()
            && instructionObject.has("destination")
            && !instructionObject.getString("destination").isEmpty()
//            !TextUtils.isEmpty(step.getDestinations()) && instructionObject.getAsJsonPrimitive("destination") != null
        ) {
            instruction = instructionObject.getString("destination");
        } else if (
            !wayName.isEmpty()
            && instructionObject.has("name")
            && !instructionObject.getString("name").isEmpty()
//            !TextUtils.isEmpty(wayName) && instructionObject.getAsJsonPrimitive("name") != null
        ) {
            instruction = instructionObject.getString("name");
        } else {
            instruction = instructionObject.getString("default");
        }

        // Replace tokens
        // NOOP if they don't exist
        String nthWaypoint = ""; // TODO, add correct waypoint counting
        String modifierValue = null;
        if (
            this.langObject.has("constants")
            && this.langObject.getJSONObject("constants").has("modifier")
            && this.langObject.getJSONObject("constants").getJSONObject("modifier").has(modifier)
        ) {
            modifierValue = this.langObject.getJSONObject("constants").getJSONObject("modifier").getString(modifier);
        }
        instruction = instruction
                .replace("{way_name}", wayName)
                .replace("{destination}", step.has("destinations") && !step.getString("destinations").isEmpty()
                        ? step.getString("destinations").split(",")[0]
                        : ""
                )
                .replace("{exit_number}", step.has("maneuver") && step.getJSONObject("maneuver").has("exit")
                        ? ordinalize(step.getJSONObject("maneuver").getInt("exit"))
                        : ordinalize(1)
                )
                .replace("{rotary_name}", step.has("name") && !step.getString("name").isEmpty()
                        ? ""
                        : step.getString("name")
                )
                .replace("{lane_instruction}", laneInstruction == null
                        ? "" :
                        laneInstruction
                )
                .replace("{modifier}", modifierValue == null
                        ? ""
                        : modifierValue
                )
                .replace("{direction}", directionFromDegree(step.getJSONObject("maneuver").getDouble("bearing_after")))
                .replace("{nth}", nthWaypoint)
                .replaceAll("\\s+", " ") // remove excess spaces
        ;

        instruction = capitalizeFirstLetter(instruction);

        return new StepTranslation(
                step.getDouble("distance"),
                step.getDouble("duration"),
                instruction
        );
    }
}