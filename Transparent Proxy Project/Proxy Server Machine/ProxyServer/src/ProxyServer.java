 import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpServer;

public class ProxyServer {
    private HttpServer server;
    private int port;

    public ProxyServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HttpRequestHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Proxy server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Proxy server stopped.");
        }
    }

    public static void main(String[] args) throws IOException {
        ProxyServer proxyServer = new ProxyServer(80);
        proxyServer.start();
    }
}
