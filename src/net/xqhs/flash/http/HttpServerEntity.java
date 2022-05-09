package net.xqhs.flash.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.sun.net.httpserver.*;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.support.MessageReceiver;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONValue;

import static java.text.MessageFormat.format;

public class HttpServerEntity extends Unit implements Entity<Node> {
    {
        setUnitName("http-server");
        setLoggerType(PlatformUtils.platformLogType());
    }
    
    private static final int SERVER_STOP_TIME = 10;
    /**
     * The {@link WebSocketServer} instance.
     */
    private HttpServer httpServer;
    /**
     * <code>true</code> if the server is currently running.
     */
    private boolean running;
    
    private HttpPylon pylon;

    protected Map<String, MessageReceiver> messageReceivers = new HashMap<>();

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

    /**
     * Creates a Http server instance. It must be started with {@link #start()}.
     * @param pylon the pylon reference
     */
    public HttpServerEntity(HttpPylon pylon) {
        this.pylon = pylon;
        pylon.li(format("Starting http server on port: {0}, resources: {1}", pylon.getServerPort(), pylon.getResourceNames()));
        try {
            httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(pylon.getServerPort()), 0);
            pylon.getResourceNames().forEach(resource -> httpServer.createContext("/" + resource, new CustomHttpHandler()));
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            httpServer.setExecutor(threadPoolExecutor);
        } catch (IOException e) {
            le(e.getMessage());
        }
    }

    @Override
    public boolean start() {
        httpServer.start();
        running = true;
        return true;
    }

    /**
     * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
     */
    @Override
    public boolean stop() {
        httpServer.stop(SERVER_STOP_TIME);
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

