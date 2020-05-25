package at.kolmann.java.FFalarmPrinter;

import com.google.maps.ImageResult;
import com.google.maps.model.DirectionsRoute;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

public class EinsatzHTML {
    private final Config config;

    public EinsatzHTML(Config config) {this.config = config;}

    public void saveEinsatz(
            String fileName,
            String einsatzID,
            JSONObject einsatz,
            String einsatzAdresse,
            DirectionsRoute route,
            ImageResult einsatzMap)
    {
        String templatePath = config.getString("htmlTemplateFile");

        if (templatePath == null) {
            return;
        }

        File templateFile = new File(templatePath);
        if (!templateFile.isAbsolute()) {
            templatePath = System.getProperty("user.dir") + File.separator + templatePath;
            templateFile = new File(templatePath);
        }

        if (!templateFile.exists()) {
            return;
        }

        StringBuilder templateSB = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(templatePath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> templateSB.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        String template = templateSB.toString();

        if (route.legs.length == 0 || route.legs[0].steps.length == 0) {
            return;
        }

        String APIkey = config.getString("googleMapsApiKeyWeb");
        if (APIkey == null) {
            APIkey = config.getString("googleMapsApiKey");
        }

        if (APIkey == null) {
            System.out.println("No Google API Key found!");
            return;
        }

        StringBuilder info = new StringBuilder();
        info.append("<div class=\"zeile\" id=\"einsatz-id\">\n");
        info.append("<div class=\"links\">\n");
        info.append("    Einsatz-ID:\n");
        info.append("  </div>\n");
        info.append("  <div class=\"rechts\">\n");
        info.append("   ").append(einsatzID).append("\n");
        info.append("  </div>\n");
        info.append("</div>\n");

        info.append("<div class=\"zeile\" id=\"einsatz-stufe\">\n");
        info.append("  <div class=\"links\">\n");
        info.append("    Alarmstufe:\n");
        info.append("  </div>\n");
        info.append("  <div class=\"rechts\">\n");
        info.append("    ").append(einsatz.getString("Alarmstufe")).append("\n");
        info.append("  </div>\n");
        info.append("</div>\n");

        info.append("<div class=\"zeile\" id=\"einsatz-bild\">\n");
        info.append("  <div class=\"links\">\n");
        info.append("    Meldebild:\n");
        info.append("  </div>\n");
        info.append("  <div class=\"rechts\">\n");
        info.append("    ").append(einsatz.getString("Meldebild")
                .replaceAll("\n", "<br />" + System.lineSeparator())).append("\n");
        info.append("  </div>\n");
        info.append("</div>\n");

        if (einsatz.has("Strasse") && einsatz.getString("Strasse").contains("A2")) {
            info.append("<div class=\"zeile\" id=\"einsatz-adresse\">\n");
            info.append("  <div class=\"links\">\n");
            info.append("    Adresse:\n");
            info.append("  </div>\n");
            info.append("  <div class=\"rechts\">\n");
            if (einsatz.has("Abschnitt")) {
                info.append("    ").append(einsatz.getString("Abschnitt")).append("<br />\n");
            }
            if (einsatz.has("Nummer1")) {
                long nr = einsatz.getLong("Nummer1");
                if (nr > 1000) {
                    nr /= 100;
                } else if (nr > 100) {
                    nr /= 10;
                }
                info.append("    Baukilometer: ").append(nr).append("<br />\n");
            }
            info.append("    ").append(einsatz.getString("Ort")).append("\n");
            info.append("  </div>\n");
            info.append("</div>\n");
        } else {
            info.append("<div class=\"zeile\" id=\"einsatz-adresse\">\n");
            info.append("  <div class=\"links\">\n");
            info.append("    Adresse:\n");
            info.append("  </div>\n");
            info.append("  <div class=\"rechts\">\n");
            info.append(einsatzAdresse.replaceAll(System.lineSeparator(), "<br />"+System.lineSeparator()));
            info.append("  </div>\n");
            info.append("</div>\n");
        }

        if (einsatz.has("Melder") && !einsatz.getString("Melder").equals("")) {
            info.append("<div class=\"zeile\" id=\"einsatz-melder\">\n");
            info.append("  <div class=\"links\">\n");
            info.append("    Melder:\n");
            info.append("  </div>\n");
            info.append("  <div class=\"rechts\">\n");
            info.append("    ").append(einsatz.getString("Melder")
                    .replaceAll("\n", "<br />" + System.lineSeparator())).append("<br />\n");
            info.append("  </div>\n");
            info.append("</div>\n");
        }

        if (einsatz.has("Bemerkung") && !einsatz.getString("Bemerkung").equals("")) {
            info.append("<div class=\"zeile\" id=\"einsatz-bemerkung\">\n");
            info.append("  <div class=\"links\">\n");
            info.append("    Bemerkung:\n");
            info.append("  </div>\n");
            info.append("  <div class=\"rechts\">\n");
            info.append("    ").append(einsatz.getString("Bemerkung")
                    .replaceAll("\n", "<br />" + System.lineSeparator())).append("<br />\n");
            info.append("  </div>\n");
            info.append("</div>\n");
        }

        // Dispositionen
        if (einsatz.get("Dispositionen") != null && einsatz.getJSONArray("Dispositionen").length() > 1) {
            info.append("<div class=\"zeile\" id=\"einsatz-alarmierteFF\">\n");
            info.append("  <div class=\"links\">\n");
            info.append("    Alarmierte Feuerwehren:\n");
            info.append("  </div>\n");
            info.append("  <div class=\"rechts\">\n");

            // Get all still active ones and sort by DispoTime
            JSONArray dispos = einsatz.getJSONArray("Dispositionen");
            JSONArray disponierteFF = new JSONArray();

            ArrayList<JSONObject> jsonValues  = new ArrayList<>();
            for (int i=0; i<dispos.length(); i++) {
                jsonValues .add(dispos.getJSONObject(i));
            }
            jsonValues.sort(new Comparator<>() {
                private static final String KEY_NAME = "DispoTime";

                @Override
                public int compare(JSONObject a, JSONObject b) {
                    String valA = "";
                    String valB = "";

                    try {
                        valA = a.getString(KEY_NAME);
                        valB = b.getString(KEY_NAME);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    return valA.compareTo(valB);
                }
            });

            for (int i = 0; i < dispos.length(); i++) {
                disponierteFF.put(jsonValues.get(i));
            }

            StringBuilder dispoList = new StringBuilder();
            Calendar myCal = Calendar.getInstance();
            String today = String.format(("%tY-%<tm-%<tdT"), myCal);
            dispoList.append("<ul>\n");

            for (int i = 0; i < disponierteFF.length(); i++) {
                JSONObject currentDispo = disponierteFF.getJSONObject(i);
                if (currentDispo.has("EinTime")) {
                    // Ignore already returned ones
                    continue;
                }
                dispoList.append("<li>");

                if (config.has("FeuerwehrName") &&
                        currentDispo.getString("Name").equals(config.getString("FeuerwehrName"))) {
                    dispoList.append("<b>");
                }

                dispoList.append(currentDispo.getString("Name"));
                dispoList.append(" (Dispo: ");
                dispoList.append(currentDispo.getString("DispoTime")
                        .replace(today, "")
                        .replace('T', ' ')
                );
                if (currentDispo.has("AlarmTime")) {
                    dispoList.append(", Alarm: ");
                    dispoList.append(currentDispo.getString("AlarmTime")
                            .replace(today, "")
                            .replace('T', ' ')
                    );
                }
                if (currentDispo.has("AusTime")) {
                    dispoList.append(", Aus: ");
                    dispoList.append(currentDispo.getString("AusTime")
                            .replace(today, "")
                            .replace('T', ' ')
                    );
                }
                dispoList.append(")");

                if (config.has("FeuerwehrName") &&
                        currentDispo.getString("Name").equals(config.getString("FeuerwehrName"))) {
                    dispoList.append("</b>");
                }
                dispoList.append("</li>\n");
            }
            dispoList.append("</ul>\n");
            info.append("    ").append(dispoList.toString());
            info.append("  </div>\n");
            info.append("</div>\n");
        }

        info.append("<div class=\"zeile\" id=\"einsatz-datum\">\n");
        info.append("  <div class=\"links\">\n");
        info.append("    Einsatzbeginn:\n");
        info.append("  </div>\n");
        info.append("  <div class=\"rechts\">\n");
        info.append("    ").append(einsatz.getString("EinsatzErzeugt").replace('T', ' ')).append("\n");
        info.append("  </div>\n");
        info.append("</div>\n");


        template = template.replaceAll("@@APIKEY@@", APIkey);
        template = template.replaceAll("@@STARTLAT@@", route.legs[0].startLocation.lat+"");
        template = template.replaceAll("@@STARTLONG@@", route.legs[0].startLocation.lng+"");
        template = template.replaceAll("@@ALARMADRESSE@@", einsatzAdresse
                .replaceAll(System.lineSeparator(), ", "));
        template = template.replaceAll("@@INPUTLISTE@@", info.toString());

        System.out.println("Saving HTML to " + fileName);
        File file = new File(fileName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(template);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
