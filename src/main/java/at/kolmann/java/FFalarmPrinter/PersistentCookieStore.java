package at.kolmann.java.FFalarmPrinter;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PersistentCookieStore implements CookieStore, Runnable {
    private final CookieStore store;
    private final String filePath;
    private final String fileName;

    public PersistentCookieStore(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;

        store = new CookieManager().getCookieStore();

        File dirPath = new File(filePath);
        File nameFile = new File(fileName);
        File file = new File(dirPath + File.separator + nameFile);

        if (file.exists()) {
            JSONArray jsonCookies = new JSONArray();
            try {
                InputStream is = new FileInputStream(dirPath + File.separator + nameFile);
                String jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);
                jsonCookies = new JSONArray(jsonTxt);
            } catch (IOException e) {
                System.out.println("Failed to read cookie file: " + e.getMessage());
            }

            for (int i = 0; i < jsonCookies.length(); i++) {
                JSONObject jsonCookie = (JSONObject) jsonCookies.get(i);
                HttpCookie cookie = new HttpCookie(
                        jsonCookie.getString("name"),
                        jsonCookie.getString("value")
                );
                if (jsonCookie.has("comment")) {
                    cookie.setComment(jsonCookie.getString("comment"));
                }

                if (jsonCookie.has("commentURL")) {
                    cookie.setCommentURL(jsonCookie.getString("commentURL"));
                }

                if (jsonCookie.has("discard")) {
                    cookie.setDiscard(jsonCookie.getBoolean("discard"));
                }

                if (jsonCookie.has("domain")) {
                    cookie.setDomain(jsonCookie.getString("domain"));
                }

                if (jsonCookie.has("maxAge")) {
                    cookie.setMaxAge(jsonCookie.getLong("maxAge"));
                }

                if (jsonCookie.has("path")) {
                    cookie.setPath(jsonCookie.getString("path"));
                }

                if (jsonCookie.has("portList")) {
                    cookie.setPortlist(jsonCookie.getString("portList"));
                }

                if (jsonCookie.has("secure")) {
                    cookie.setSecure(jsonCookie.getBoolean("secure"));
                }

                if (jsonCookie.has("version")) {
                    cookie.setVersion(jsonCookie.getInt("version"));
                }

                StringBuilder uri = new StringBuilder();
                if (jsonCookie.getBoolean("secure")) {
                    uri.append("https://");
                } else {
                    uri.append("https://");
                }

                uri.append(jsonCookie.getString("domain"));
                if (jsonCookie.has("path")) {
                    uri.append(jsonCookie.getString("path"));
                }

                store.add(URI.create(uri.toString()), cookie);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }

    // Shutdown Hook Runner
    @Override
    public void run() {
        File dirPath = new File(filePath);
        File nameFile = new File(fileName);
        File file = new File(dirPath + File.separator + nameFile);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // serialize cookies to persistent storage
        JSONArray jsonCookies = new JSONArray();
        for (HttpCookie cookie : store.getCookies()) {
            jsonCookies.put(getJsonCookie(cookie));
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file,true));
            writer.write(jsonCookies.toString());
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject getJsonCookie(HttpCookie cookie) {
        JSONObject jsonCookie = new JSONObject();
        jsonCookie.put("comment", cookie.getComment());
        jsonCookie.put("commentURL", cookie.getCommentURL());
        jsonCookie.put("discard", cookie.getDiscard());
        jsonCookie.put("domain", cookie.getDomain());
        jsonCookie.put("maxAge", cookie.getMaxAge());
        jsonCookie.put("name", cookie.getName());
        jsonCookie.put("path", cookie.getPath());
        jsonCookie.put("portList", cookie.getPortlist());
        jsonCookie.put("secure", cookie.getSecure());
        jsonCookie.put("value", cookie.getValue());
        jsonCookie.put("version", cookie.getVersion());
        return jsonCookie;
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        store.add(uri, cookie);
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        return store.get(uri);
    }

    @Override
    public List<HttpCookie> getCookies() {
        return store.getCookies();
    }

    @Override
    public List<URI> getURIs() {
        return store.getURIs();
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        return store.remove(uri, cookie);
    }

    @Override
    public boolean removeAll() {
        return store.removeAll();
    }
}
