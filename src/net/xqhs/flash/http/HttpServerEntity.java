package net.xqhs.flash.http;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.*;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.support.MessageReceiver;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONValue;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import static java.text.MessageFormat.format;

public class HttpServerEntity extends Unit implements Entity<Node> {
    {
        setUnitName("http-server");
        setLoggerType(PlatformUtils.platformLogType());
    }
    
    private static final int SERVER_STOP_TIME = 10;
    /**
     * The {@link HttpServer} instance.
     */
    private HttpServer httpServer;

    /**
     * If the server should be deployed with TLS feature or not
     */
    private boolean isHttps;

    /**
     * The {@link HttpsServer} instance
     */
    private HttpsServer httpsServer;
    /**
     * <code>true</code> if the server is currently running.
     */
    private boolean running;

    /**
     * The pylon associated with this entity
     */
    private HttpPylon pylon;

    class CustomHttpHandler extends Unit implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET"))
            {
                displayInfo(exchange);
            } else {
                receiveMessage(exchange);
            }
        }

        private void displayInfo(HttpExchange exchange) throws IOException
        {
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            String response = "Hello from " + exchange.getHttpContext().getPath().substring(1) + "!";
            os.write(response.getBytes());
            os.close();
        }

        private void receiveMessage(HttpExchange exchange)
        {
            try
            {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                Object obj = JSONValue.parse(query);
                if(obj == null) {
                    pylon.le("Received message is null.");
                    return;
                }
                JSONObject jsonObject = (JSONObject) obj; 
                String response = "";
                exchange.sendResponseHeaders(200, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                
                jsonObject.put("sourceAddress", exchange.getRequestHeaders().get("sourceAddress").get(0));
                pylon.receiveMessage(jsonObject);
            }
            catch (IOException e)
            {
                pylon.le("error when decoding the message", e);
            }
        }
    }
    
    public HttpServerEntity(HttpPylon pylon) {
        this(pylon, false, null);
    }

    /**
     * Creates a Http server instance. It must be started with {@link #start()}.
     * @param pylon the pylon reference
     * @param isHttps if server should be configured with TLS
     * @param certificatePath certificate of the HTTPS server
     */
    public HttpServerEntity(HttpPylon pylon, boolean isHttps, String certificatePath) {
        this.pylon = pylon;
        this.isHttps = isHttps;
        pylon.li(format("Starting {0} server on port: {1}, resources: {2}", isHttps ? "https" : "http", pylon.getServerPort(), pylon.getResourceNames()));
        if (isHttps) {
            createHttpsServer(certificatePath);
        } else {
            createHttpServer();
        }
    }
    
    private void createHttpServer() {
        try {
            httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(pylon.getServerPort()), 0);
            pylon.getResourceNames().forEach(resource -> httpServer.createContext("/" + resource, new CustomHttpHandler()));
            httpServer.setExecutor(Executors.newFixedThreadPool(10));
        } catch (IOException e) {
            le(e.getMessage());
        }
    }
    
    private void createHttpsServer(String certPath) {
        try (InputStream fis = this.getClass().getResourceAsStream(certPath)){
            httpsServer = HttpsServer.create(new InetSocketAddress(pylon.getServerPort()), 0);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            char[] password = "password".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(fis, password);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            HttpsConfigurator httpsConfigurator = new HttpsCustomConfigurator(sslContext);
            httpsServer.setHttpsConfigurator(httpsConfigurator);
            httpsServer.setExecutor(Executors.newFixedThreadPool(10));
            pylon.getResourceNames().forEach(resource -> httpsServer.createContext("/" + resource, new CustomHttpHandler()));
        }
        catch (Exception e)
        {
            le(e.getMessage());
        }
    }
    

    @Override
    public boolean start() {
        if (isHttps) {
            httpsServer.start();
        } else {
            httpServer.start();
        }
        running = true;
        return true;
    }

    /**
     * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
     */
    @Override
    public boolean stop() {
        if (isHttps) {
            httpsServer.stop(SERVER_STOP_TIME);
        } else {
            httpServer.stop(SERVER_STOP_TIME);
        }
        running = false;
        li("server successfully stopped.");
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public boolean addContext(EntityProxy<Node> context)
    {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Node> context)
    {
        return false;
    }

    @Override
    public <C extends Entity<Node>> EntityProxy<C> asContext()
    {
        return null;
    }

    @Override
    protected void li(String message, Object... arguments) {
        super.li(message, arguments);
    }

    @Override
    protected void lw(String message, Object... arguments) {
        super.lw(message, arguments);
    }

    @Override
    protected void le(String message, Object... arguments) {
        super.le(message, arguments);
    }
}

