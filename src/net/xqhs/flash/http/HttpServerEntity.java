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
import net.xqhs.flash.core.support.MessageReceiver;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONValue;

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

    static class CustomHttpHandler extends Unit implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            printRequestInfo(exchange);
            receiveMessage(exchange);
        }
        
        private void receiveMessage(HttpExchange exchange) throws IOException
        {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String query = br.readLine();
            le(query);
            Object obj = JSONValue.parse(query);
            if(obj == null)
                return;
            JSONObject message = (JSONObject) obj;
            Object destination = message.get("destination");
            Object source = message.get("source");
            JSONObject resp = new JSONObject();
            resp.put("destination", source);
            resp.put("source", destination);
            resp.put("content", "OK");
            
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/plain;charset=UTF-8");
            String response = resp.toString();
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private void printRequestInfo(HttpExchange exchange) {
//            System.out.println("-- headers --");
//            Headers requestHeaders = exchange.getRequestHeaders();
//            requestHeaders.entrySet().forEach(System.out::println);
//
//            System.out.println("-- principal --");
//            HttpPrincipal principal = exchange.getPrincipal();
//            System.out.println(principal);
//
//            System.out.println("-- HTTP method --");
//            String requestMethod = exchange.getRequestMethod();
//            System.out.println(requestMethod);
//
//            System.out.println("-- query --");
//            URI requestURI = exchange.getRequestURI();
//            String query = requestURI.getQuery();
//            System.out.println(query);
        }
    }
    
    
    /**
     * Creates a Websocket server instance. It must be started with {@link #start()}.
     *
     * @param serverPort
     *            - the port on which to start the server.
     */
    public HttpServerEntity(int serverPort, Map<String, MessageReceiver> messageReceivers) {
        li("Starting http server on port: ", serverPort);
        try {
            httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(serverPort), 0);
            httpServer.createContext("/", new CustomHttpHandler());
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
    public boolean addContext(EntityProxy<Node> context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Node> context) {
        return false;
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
    public <C extends Entity<Node>> EntityProxy<C> asContext() {
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

