package at.kolmann.java.FFalarmPrinter;

import org.json.JSONObject;

import java.io.*;

public class Config {
    private JSONObject config;

    public Config() {
        LoadConfig(null);
    }
    public Config(String configFileString) {
        LoadConfig(configFileString);
    }

    private void LoadConfig(String configFileString) {
        File configFile = null;
        if (configFileString != null) {
            configFile = new File(configFileString);

            if (!configFile.isAbsolute()) {
                configFile = new File(System.getProperty("user.dir") + File.separator + configFileString);
            }
        }

        if (configFile == null || !configFile.exists()) {
            configFile = new File(System.getProperty("user.dir") + File.separator + "config.json");
        }

        if (configFile.exists()) {
            StringBuilder configString = new StringBuilder();

            try {
                InputStreamReader inputreader = new InputStreamReader(new FileInputStream(configFile));
                BufferedReader bufferedReader = new BufferedReader(inputreader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    configString.append(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            config = new JSONObject(configString.toString());

        } else {
            System.out.println("No config found....");
            System.out.println("Loading default values...");
            config = new JSONObject();
            config.put("cookieFile", "cookies.txt");
            config.put("wastlUrl", "https://infoscreen.florian10.info/ows/infoscreen/demo.ashx?demo=1");
        }
    }

    public Object get(String key) {
        if (config.has(key)) {
            return config.get(key);
        } else {
            return null;
        }
    }

    public String getString(String key) {
        Object value = get(key);
        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }

    public double getDouble(String key) throws IOException {
        Object value = get(key);
        if (value instanceof Double) {
            return (double) value;
        }

        throw new IOException("Key " + key + " is not a double!");
    }

    public Boolean has(String key) {
        return config.has(key);
    }
}
