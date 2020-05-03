package at.kolmann.java.FFalarmPrinter;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.StringTokenizer;

public class PersistentCookieStore implements CookieStore, Runnable {
    private CookieStore store;
    private String filePath;
    private String fileName;

    public PersistentCookieStore(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;

        store = new CookieManager().getCookieStore();

        int i = 0;
        String line;
        String cookieURI = null;
        String name = null;
        String value = null;
        String domain = null;
        String path = null;
        String expired = null;
        String version = null;
        File dirPath = new File(filePath);
        File nameFile = new File(fileName);
        File file = new File(dirPath + File.separator + nameFile);

        if (file.exists()) {
            try {
                InputStreamReader inputreader = new InputStreamReader(new FileInputStream(file));

                BufferedReader bufferedReader = new BufferedReader(inputreader);

                while ((line = bufferedReader.readLine()) != null) {
                    StringTokenizer tokens = new StringTokenizer(line, "#");
                    i++;
                    while (tokens.hasMoreTokens()) {
                        switch (i = tokens.countTokens()) {
                            case 7:
                                cookieURI = tokens.nextToken();
                                break;
                            case 6:
                                name = tokens.nextToken();
                                break;
                            case 5:
                                value = tokens.nextToken();
                                break;
                            case 4:
                                domain = tokens.nextToken();
                                break;
                            case 3:
                                path = tokens.nextToken();
                                break;
                            case 2:
                                version = tokens.nextToken();
                                break;
                            case 1:
                                expired = tokens.nextToken();
                                break;
                        }
                    }


                    HttpCookie cookie = new HttpCookie(name, value);
                    if (value.contentEquals("*"))
                        cookie.setValue(null);
                    cookie.setDomain(domain);
                    cookie.setPath(path);
                    cookie.setVersion(Integer.parseInt(version));
                    cookie.setMaxAge(Long.parseLong(expired));

                    store.add(new URI(cookieURI), cookie);
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }

    @Override
    public void run() {
        String SEPARATOR = "#";

        // serialize cookies to persistent storage
        System.out.println("");
        System.out.println("");

        List<URI> cookieURIs = store.getURIs();
        for (URI uri : cookieURIs) {
            List<HttpCookie> cookies = store.get(uri);

            for (HttpCookie cookie : cookies) {
                String cookieURI = uri.toASCIIString();

                // https://github.com/augustopicciani/HttpClient-save-cookies-to-file/blob/master/src/com/httpclient_save_cookies/CookieHelper.java
                String name = cookie.getName();
                String value = "*";
                if (cookie.getValue() != null && !cookie.getValue().contentEquals("")) {
                    value = cookie.getValue();
                }

                String domain = "*";
                if (cookie.getDomain() != null) {
                    domain = cookie.getDomain();
                }

                String path = "*";
                if (cookie.getPath() != null) {
                    path = cookie.getPath();
                }

                int version = cookie.getVersion();
                String ver = String.valueOf(version);

                String expired = "*";
                if (cookie.getMaxAge() != 0) {
                    expired = Long.toString(cookie.getMaxAge());
                }

                File dirPath = new File(filePath);
                File nameFile = new File(fileName);
                File file = new File(dirPath + File.separator + nameFile);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(cookieURI);
                    writer.write(SEPARATOR);
                    writer.write(name);
                    writer.write(SEPARATOR);
                    writer.write(value);
                    writer.write(SEPARATOR);
                    writer.write(domain);
                    writer.write(SEPARATOR);
                    writer.write(path);
                    writer.write(SEPARATOR);
                    writer.write(ver);
                    writer.write(SEPARATOR);
                    writer.write(expired);
                    writer.write(SEPARATOR);
                    writer.newLine();
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
