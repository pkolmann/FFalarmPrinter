package at.kolmann.java.FFalarmPrinter;

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
            StringBuilder langString = new StringBuilder();

            try {
                InputStreamReader inputReader = new InputStreamReader(new FileInputStream(languageFile));
                BufferedReader bufferedReader = new BufferedReader(inputReader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    langString.append(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONObject loadedObject = new JSONObject(langString.toString());
            String mainLangKey = "v5";
            if (loadedObject.has(mainLangKey)) {
                this.langObject = loadedObject.getJSONObject(mainLangKey);
            }

        } else {
            System.out.println("No languageFile found....");
            System.out.println("Loading default values...");
        }

        System.out.println(this.langObject.toString());
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
            return this.langObject.getJSONObject("constants").getJSONObject("ordinalize")
                    .getString(String.valueOf(number));
        } catch (Exception exception) {
            return "";
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
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("north");
        } else if (degree > 20 && degree < 70) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("northeast");
        } else if (degree >= 70 && degree <= 110) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("east");
        } else if (degree > 110 && degree < 160) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("southeast");
        } else if (degree >= 160 && degree <= 200) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("south");
        } else if (degree > 200 && degree < 250) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("southwest");
        } else if (degree >= 250 && degree <= 290) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("west");
        } else if (degree > 290 && degree < 340) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("northwest");
        } else if (degree >= 340 && degree <= 360) {
            return this.langObject.getJSONObject("constants").getJSONObject("direction")
                    .getString("north");
        } else {
            throw new RuntimeException("Degree is invalid: " + degree);
        }
    }

    /**
     * Reduce any lane combination down to a contracted lane diagram
     *
     * @param step a route step
     */
//    public String laneConfig(LegStep step) {
//        if (step.getIntersections() == null
//                || step.getIntersections().size() == 0
//                || step.getIntersections().get(0).getLanes() == null
//                || step.getIntersections().get(0).getLanes().length == 0) {
//            throw new RuntimeException("No lanes object");
//        }
//
//        StringBuilder config = new StringBuilder();
//        Boolean currentLaneValidity = null;
//        for (IntersectionLanes lane : step.getIntersections().get(0).getLanes()) {
//            if (currentLaneValidity == null || currentLaneValidity != lane.getValid()) {
//                if (lane.getValid()) {
//                    config.append("o");
//                } else {
//                    config.append("x");
//                }
//                currentLaneValidity = lane.getValid();
//            }
//        }
//
//        return config.toString();
//    }
//
//    public String compile(LegStep step) {
//        if (step.getManeuver() == null) {
//            throw new RuntimeException("No step maneuver provided.");
//        }
//
//        String type = step.getManeuver().getType();
//        String modifier = step.getManeuver().getModifier();
//        String mode = step.getMode();
//
//        if (TextUtils.isEmpty(type)) {
//            throw new RuntimeException("Missing step maneuver type.");
//        }
//
//        if (!type.equals("depart") && !type.equals("arrive") && TextUtils.isEmpty(modifier)) {
//            throw new RuntimeException("Missing step maneuver modifier.");
//        }
//
//        if (getVersionObject().getAsJsonObject(type) == null) {
//            // Log for debugging
//            logger.log(Level.FINE, "Encountered unknown instruction type: " + type);
//
//            // OSRM specification assumes turn types can be added without
//            // major version changes. Unknown types are to be treated as
//            // type `turn` by clients
//            type = "turn";
//        }
//
//        // Use special instructions if available, otherwise `defaultinstruction`
//        JsonObject instructionObject;
//        JsonObject modeValue = getVersionObject().getAsJsonObject("modes").getAsJsonObject(mode);
//        if (modeValue != null) {
//            instructionObject = modeValue;
//        } else {
//            JsonObject modifierValue = getVersionObject().getAsJsonObject(type).getAsJsonObject(modifier);
//            instructionObject = modifierValue == null
//                    ? getVersionObject().getAsJsonObject(type).getAsJsonObject("default")
//                    : modifierValue;
//        }
//
//        // Special case handling
//        JsonPrimitive laneInstruction = null;
//        switch (type) {
//            case "use lane":
//                laneInstruction = getVersionObject().getAsJsonObject("constants")
//                        .getAsJsonObject("lanes").getAsJsonPrimitive(laneConfig(step));
//                if (laneInstruction == null) {
//                    // If the lane combination is not found, default to continue straight
//                    instructionObject = getVersionObject().getAsJsonObject("use lane")
//                            .getAsJsonObject("no_lanes");
//                }
//                break;
//            case "rotary":
//            case "roundabout":
//                if (!TextUtils.isEmpty(step.getRotaryName())
//                        && step.getManeuver().getExit() != null
//                        && instructionObject.getAsJsonObject("name_exit") != null) {
//                    instructionObject = instructionObject.getAsJsonObject("name_exit");
//                } else if (step.getRotaryName() != null && instructionObject.getAsJsonObject("name") != null) {
//                    instructionObject = instructionObject.getAsJsonObject("name");
//                } else if (step.getManeuver().getExit() != null && instructionObject.getAsJsonObject("exit") != null) {
//                    instructionObject = instructionObject.getAsJsonObject("exit");
//                } else {
//                    instructionObject = instructionObject.getAsJsonObject("default");
//                }
//                break;
//            default:
//                // NOOP, since no special logic for that type
//        }
//
//        // Decide way_name with special handling for name and ref
//        String wayName;
//        String name = TextUtils.isEmpty(step.getName()) ? "" : step.getName();
//        String ref = TextUtils.isEmpty(step.getRef()) ? "" : step.getRef().split(";")[0];
//
//        // Remove hacks from Mapbox Directions mixing ref into name
//        if (name.equals(step.getRef())) {
//            // if both are the same we assume that there used to be an empty name, with the ref being filled in for it
//            // we only need to retain the ref then
//            name = "";
//        }
//        name = name.replace(" (" + step.getRef() + ")", "");
//
//        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(ref) && !name.equals(ref)) {
//            wayName = name + " (" + ref + ")";
//        } else if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(ref)) {
//            wayName = ref;
//        } else {
//            wayName = name;
//        }
//
//        // Decide which instruction string to use
//        // Destination takes precedence over name
//        String instruction;
//        if (!TextUtils.isEmpty(step.getDestinations())
//                && instructionObject.getAsJsonPrimitive("destination") != null) {
//            instruction = instructionObject.getAsJsonPrimitive("destination").getAsString();
//        } else if (!TextUtils.isEmpty(wayName)
//                && instructionObject.getAsJsonPrimitive("name") != null) {
//            instruction = instructionObject.getAsJsonPrimitive("name").getAsString();
//        } else {
//            instruction = instructionObject.getAsJsonPrimitive("default").getAsString();
//        }
//
//        if (getTokenizedInstructionHook() != null) {
//            instruction = getTokenizedInstructionHook().change(instruction);
//        }
//
//        // Replace tokens
//        // NOOP if they don't exist
//        String nthWaypoint = ""; // TODO, add correct waypoint counting
//        JsonPrimitive modifierValue =
//                getVersionObject().getAsJsonObject("constants").getAsJsonObject("modifier").getAsJsonPrimitive(modifier);
//        instruction = instruction
//                .replace("{way_name}", wayName)
//                .replace("{destination}", TextUtils.isEmpty(step.getDestinations()) ? "" : step.getDestinations().split(",")[0])
//                .replace("{exit_number}",
//                        step.getManeuver().getExit() == null ? ordinalize(1) : ordinalize(step.getManeuver().getExit()))
//                .replace("{rotary_name}", TextUtils.isEmpty(step.getRotaryName()) ? "" : step.getRotaryName())
//                .replace("{lane_instruction}", laneInstruction == null ? "" : laneInstruction.getAsString())
//                .replace("{modifier}", modifierValue == null ? "" : modifierValue.getAsString())
//                .replace("{direction}", directionFromDegree(step.getManeuver().getBearingAfter()))
//                .replace("{nth}", nthWaypoint)
//                .replaceAll("\\s+", " "); // remove excess spaces
//
//        if (getRootObject().getAsJsonObject("meta").getAsJsonPrimitive("capitalizeFirstLetter").getAsBoolean()) {
//            instruction = capitalizeFirstLetter(instruction);
//        }
//
//        return instruction;
//    }
}