package at.kolmann.java.FFalarmPrinter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.stream.Stream;

public class EinsatzHTML {
    private final Config config;

    public EinsatzHTML(Config config) {this.config = config;}

    public void saveEinsatz(
            String fileName,
            String einsatzID,
            JSONObject einsatz,
            JSONArray disponierteFF,
            String einsatzAdresse,
            Double einsatzLong,
            Double einsatzLat,
            String routeJson,
            String hydrantsJson,
            String routeSteps)
    {
        String templatePath = config.getString("htmlTemplateFile");

        if (templatePath == null) {
            System.out.println("No htmlTemplateFile set in config.json. (Set to 'none' to hide this message)");
            return;
        }

        if (templatePath.equalsIgnoreCase("none")) {
            return;
        }

        File templateFile = new File(templatePath);
        if (!templateFile.isAbsolute()) {
            templatePath = System.getProperty("user.dir") + File.separator + templatePath;
            templateFile = new File(templatePath);
        }

        if (!templateFile.exists()) {
            System.out.println("TemplateFile " + templatePath + " does not exist!");
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
                double nr = einsatz.getDouble("Nummer1");
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

        if (einsatz.has("Melder") && !einsatz.getString("Melder").isEmpty()) {
            info.append("<div class=\"zeile\" id=\"einsatz-melder\">\n");
            info.append("  <div class=\"links\">\n");
            info.append("    Melder:\n");
            info.append("  </div>\n");
            info.append("  <div class=\"rechts\">\n");

            StringBuilder melder = new StringBuilder();
            melder.append(einsatz.getString("Melder")
                    .replaceAll("\n", "<br />" + System.lineSeparator()));
            if (einsatz.has("MelderTelefon") && !einsatz.getString("MelderTelefon").isEmpty()) {
                melder.append(" (").append(einsatz.getString("MelderTelefon")).append(")");
            }
            info.append("    ").append(melder).append("<br />\n");
            info.append("  </div>\n");
            info.append("</div>\n");
        }

        if (einsatz.has("Bemerkung") && !einsatz.getString("Bemerkung").isEmpty()) {
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
        if (disponierteFF != null) {
            info.append("<div class=\"zeile\" id=\"einsatz-alarmierteFF\">\n");
            info.append("  <div class=\"links\">\n");
            info.append("    Alarmierte Feuerwehren:\n");
            info.append("  </div>\n");
            info.append("  <div class=\"rechts\">\n");

            StringBuilder dispoList = new StringBuilder();
            Calendar myCal = Calendar.getInstance();
            String today = String.format(("%tY-%<tm-%<tdT"), myCal);
            dispoList.append("<ul>\n");

            boolean hasActiveFF = false;
            for (int i = 0; i < disponierteFF.length(); i++) {
                JSONObject currentDispo = disponierteFF.getJSONObject(i);
                if (!currentDispo.has("EinTime")) {
                    // Ignore already returned ones
                    hasActiveFF = true;
                }
            }
            for (int i = 0; i < disponierteFF.length(); i++) {
                JSONObject currentDispo = disponierteFF.getJSONObject(i);
                if (hasActiveFF && currentDispo.has("EinTime")) {
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
            info.append("    ").append(dispoList);
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

        String feuerwehrhausLocationLat = null;
        String feuerwehrhausLocationLon = null;
        try {
            feuerwehrhausLocationLat = String.valueOf(config.getDouble("FeuerwehrhausLocationLat"));
            feuerwehrhausLocationLon = String.valueOf(config.getDouble("FeuerwehrhausLocationLon"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (feuerwehrhausLocationLat == null) {
            System.out.println("EinsatzHTML: FeuerwehrhausLocationLat not found in config!");
            feuerwehrhausLocationLat = "48.0";
        }
        if (feuerwehrhausLocationLon == null) {
            System.out.println("EinsatzHTML: FeuerwehrhausLocationLon not found in config!");
            feuerwehrhausLocationLon = "16.0";
        }

        template = template.replaceAll("@@STARTLAT@@", feuerwehrhausLocationLat);
        template = template.replaceAll("@@STARTLONG@@", feuerwehrhausLocationLon);
        template = template.replaceAll("@@ENDLAT@@", einsatzLat.toString());
        template = template.replaceAll("@@ENDLONG@@", einsatzLong.toString());
        template = template.replaceAll("@@ROUTEJSON@@", escape(routeJson));
        template = template.replaceAll("@@HYDRANTSJSON@@", escape(hydrantsJson));
        template = template.replaceAll("@@INPUTLISTE@@", info.toString());
        template = template.replaceAll("@@ROUTESTEPS@@", escape(routeSteps));

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

    // https://stackoverflow.com/a/50522874
    private String escape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        // TODO: escape other non-printing characters using uXXXX notation
        return escaped;
    }
}
