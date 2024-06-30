 import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class HttpRequestHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(HttpRequestHandler.class.getName());
    private static final String BLOCKED_HOSTS_FILE = "blocked_hosts.txt";
    private static final String CACHE_DIR = "cache/";
    private static final String LOG_FILE = "proxy_logs.txt";
    private static FileHandler fileHandler;

    static {
        try {
            fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

            // Ensure the cache directory exists
            File cacheDir = new File(CACHE_DIR);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String hostHeader = exchange.getRequestHeaders().getFirst("Host");
        String requestUrl = "http://" + hostHeader + exchange.getRequestURI().toString();
        URL url = new URL(requestUrl);

        if (isBlocked(url.getHost())) {
            sendBlockedResponse(exchange);
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            File cachedFile = getCachedFile(url);
            if (cachedFile.exists() && isCacheable(cachedFile)) {
                String lastModified = getLastModified(cachedFile);
                if (lastModified != null) {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("If-Modified-Since", lastModified);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                        serveFromCache(exchange, cachedFile);
                        logRequest(exchange, method, url, responseCode);
                        return;
                    }
                }
            }
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);

        // Copy request headers from the client to the connection
        exchange.getRequestHeaders().forEach((key, values) -> values.forEach(value -> connection.addRequestProperty(key, value)));

        int responseCode = connection.getResponseCode();
        exchange.sendResponseHeaders(responseCode, connection.getContentLength());

        try (InputStream is = connection.getInputStream();
             OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            if ("GET".equalsIgnoreCase(method) && responseCode == HttpURLConnection.HTTP_OK) {
                try (FileOutputStream fos = new FileOutputStream(getCachedFile(url))) {
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                updateLastModified(getCachedFile(url), connection.getHeaderField("Last-Modified"));
            } else {
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            try (InputStream es = connection.getErrorStream();
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (es != null && (bytesRead = es.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }

        logRequest(exchange, method, url, responseCode);
    }

    private boolean isBlocked(String host) throws IOException {
        File file = new File(BLOCKED_HOSTS_FILE);
        if (!file.exists()) {
            file.createNewFile();
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(host)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendBlockedResponse(HttpExchange exchange) throws IOException {
        String response = "This site is blocked.";
        exchange.sendResponseHeaders(403, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void logRequest(HttpExchange exchange, String method, URL url, int responseCode) {
        String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();
        String logMessage = String.format("Received request: %s %s from %s - Response: %d", method, url, clientIP, responseCode);
        logger.info(logMessage);
        System.out.println(logMessage);
    }

    private boolean isCacheable(File cachedFile) {
        return cachedFile.exists();
    }

    private File getCachedFile(URL url) {
        String fileName = url.toString().replaceAll("[^a-zA-Z0-9]", "_");
        return new File(CACHE_DIR + fileName);
    }

    private void serveFromCache(HttpExchange exchange, File cachedFile) throws IOException {
        exchange.sendResponseHeaders(200, cachedFile.length());
        try (InputStream is = new FileInputStream(cachedFile);
             OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    private String getLastModified(File cachedFile) {
        File lastModifiedFile = new File(cachedFile.getAbsolutePath() + ".lastmodified");
        if (lastModifiedFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(lastModifiedFile))) {
                return br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void updateLastModified(File cachedFile, String lastModified) {
        if (lastModified != null) {
            File lastModifiedFile = new File(cachedFile.getAbsolutePath() + ".lastmodified");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(lastModifiedFile))) {
                bw.write(lastModified);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
