package at.kolmann.java.FFalarmPrinter;
import java.io.*;
import java.util.*;

public class ArchivePageGenerator {
    private final Config config;

    public ArchivePageGenerator(Config config) {
        this.config = config;

        // Check if archiv.html already has been generated, if not create it
        this.generate(true);
    }

    public void generate() {
        generate(false);
    }

    public void generate(boolean firstRun) {
        File savePathFile;

        String savePath = config.getString("saveWEBlocation");
        if (savePath == null) {
            System.out.println("saveWEBlocation not defined in config!");
            return;
        }

        savePathFile = new File(savePath);
        if (!savePathFile.isAbsolute()) {
            savePath = System.getProperty("user.dir") + File.separator + savePath;
            savePathFile = new File(savePath);
        }

        if (!savePathFile.exists()) {
            System.out.println("Can't find " + savePath);
            return;
        }

        File archivFile = new File(savePath + File.separator + "archiv.html");
        if (firstRun && archivFile.exists()) {
            return;
        }

        StringBuilder archiv = new StringBuilder();
        archiv.append("<!DOCTYPE html>\n");
        archiv.append("<html lang=\"de\">\n");
        archiv.append("<head>\n");
        archiv.append("    <meta charset=\"UTF-8\">\n");
        archiv.append("    <title>FF Archiv</title>\n");
        archiv.append("    <link href=\"template.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
        archiv.append("</head>\n");
        archiv.append("<body>\n");
        archiv.append("<div id=\"page\" class=\"page\">\n");
        archiv.append("    <div id=\"back-link\">\n");
        archiv.append("         <a href=\"index.html\">Zum Index</a>\n");
        archiv.append("    </div>\n");
        archiv.append("    <div id=\"header\">\n");
        archiv.append("        <h1>FF Archiv</h1>\n");
        archiv.append("    </div>\n");
        archiv.append("    <div id=\"archive-list\">\n");

        File[] files1 = savePathFile.listFiles();
        if (files1 != null) {
            Arrays.sort(files1, (a, b) -> -a.getName().compareTo(b.getName()));
            for (File file1 : files1) {
                if (file1.isDirectory()) {
                    String dirName = file1.getName();
                    File[] files = file1.listFiles((dir, name) -> name.toLowerCase().startsWith("alarm-"));
                    if (files == null) {
                        continue;
                    }

                    archiv.append("    <h3>").append(dirName).append("</h3>\n");
                    archiv.append("        <ul>\n");
                    TreeMap<String, ArrayList<String>> alarmFiles = new TreeMap<>();
                    for (File file : files) {
                        String fileName = file.getName();
                        if (!fileName.startsWith("alarm-")) {
                            continue;
                        }
                        if (!fileName.endsWith(".html") && !fileName.endsWith(".pdf")) {
                            continue;
                        }

                        String fileBase = fileName.substring(0, fileName.lastIndexOf('.'));
                        String fileExt = fileName.substring(fileName.lastIndexOf('.'));
                        if (!alarmFiles.containsKey(fileBase)) {
                            ArrayList<String> listToAdd = new ArrayList<>();
                            listToAdd.add(fileExt);
                            alarmFiles.put(fileBase, listToAdd);
                        } else {
                            ArrayList<String> listToAdd = alarmFiles.get(fileBase);
                            listToAdd.add(fileExt);
                            alarmFiles.replace(fileBase, listToAdd);
                        }
                    }

                    ArrayList<String> keys = new ArrayList<>(alarmFiles.keySet());
                    for (int i=keys.size()-1; i>=0;i--){
                        archiv.append("<li>");
                        String alarmFile = keys.get(i);
                        ArrayList<String> extenstions = alarmFiles.get(alarmFile);

                        if (extenstions.contains(".html")) {
                            archiv.append("<a href=\"").append(dirName).append("/").append(alarmFile).append(".html\">");
                            archiv.append(alarmFile).append(".html</a>");
                        }

                        if (extenstions.contains(".pdf")) {
                            archiv.append(" / ");
                            archiv.append("<a href=\"").append(dirName).append("/").append(alarmFile).append(".pdf\">");
                            archiv.append(alarmFile).append(".pdf</a>");
                        }

                        archiv.append("</li>");
                        archiv.append("\n");
                    }

                    archiv.append("        </ul>\n");
                    archiv.append("        <br/><br/>\n");

                }
            }
        }


        Calendar myCal = Calendar.getInstance();
        String today = String.format(("%td. %<tm. %<tY %<tH:%<tM:%<tS"), myCal);
        archiv.append("</div>\n");
        archiv.append("<div id=\"div-generiert\">\n");
        archiv.append("generiert: ").append(today).append("\n");
        archiv.append("</div>\n");

        archiv.append("    </div>\n");
        archiv.append("</body>\n");
        archiv.append("</html>\n");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(archivFile));
            writer.write(archiv.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
