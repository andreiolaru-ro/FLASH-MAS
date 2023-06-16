package httphomeserver;

import com.sun.net.httpserver.*;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.http.HttpsCustomConfigurator;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static httphomeserver.RegionsHttpAgentStatus.Status.*;
import static httphomeserver.RegionsHttpMessageFactory.MessageType.REQ_ACCEPT;
import static httphomeserver.RegionsHttpMessageFactory.MessageType.REQ_BUFFER;
import static java.text.MessageFormat.format;

public class RegionsHttpServerEntity extends Unit implements Entity<Node> {
    {
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
    private final boolean isHttps;

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
    private final RegionsHttpPylon pylon;

    /**
     * List of agents with the birthplace in this region.
     */
    private final Map<String, RegionsHttpAgentStatus> homeServerAgents = Collections.synchronizedMap(new HashMap<>());
    /**
     * List of the agents that arrived in this region.
     */
    private final Map<String, RegionsHttpAgentStatus>	mobileAgents		= Collections.synchronizedMap(new HashMap<>());

    public void printStatus() {
        lf("region agents:[] guest agents:[]", homeServerAgents, mobileAgents);
    }

    private final HttpClient httpClient;

    @Override
    public String getUnitName() {
        return "http://" + this.pylon.getServerHost() + ":" + this.pylon.getServerPort();
    }


    public void processResponse(CompletableFuture<HttpResponse<String>> responseWrapper, String url, String message) {
        Runnable runnable = () -> {
            try {
                HttpResponse<String> resp = responseWrapper.get();
                if (resp.statusCode() != 200) {
                    le("Status code different than 200 when calling url []. Message: []. Response body: [].", url, message, resp.body());
                }
            } catch (InterruptedException | ExecutionException e) {
                le("Error when calling url []. Message: [].", url, message);
            }
        };
        Thread t = new Thread(runnable);
        t.start();
    }
     class MessageHandler {

         /**
          * A method that checks if the connection is still open before sending a message.
          *
          * @param url
          *            - the destination name
          * @param message
          *            - the message that will be sent
          */
        public void sendMessage(String url, String message) {
            try {
                URI uri = new URI(url);
                HttpRequest httpRequest = HttpRequest.newBuilder().header("Content-Type", "text/plain;charset=UTF-8")
                        .uri(uri)
                        .POST(HttpRequest.BodyPublishers.ofString(message)).build();
                CompletableFuture<HttpResponse<String>> resp = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
                processResponse(resp, url, message);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                le("Error when calling url " + url);
                throw e;
            }
        }

        public void registerMessageHandler(JSONObject mesg) {
            String new_agent = (String) mesg.get("source");
            lf("Received REGISTER message from new agent ", new_agent);
            if (homeServerAgents.containsKey(new_agent)) {
                le("An agent with the name [] already existed!", new_agent);
            } else {
                register(new_agent);
            }
            printStatus();
        }

        public void connectMessageHandler(JSONObject mesg) {
            String arrived_agent = (String) mesg.get("source");
            lf("Received CONNECT message from mobile agent ", arrived_agent);
            if (!homeServerAgents.containsKey(arrived_agent)) {
                if (!mobileAgents.containsKey(arrived_agent)) {
                    mobileAgents.put(arrived_agent, new RegionsHttpAgentStatus(arrived_agent, RegionsHttpAgentStatus.Status.REMOTE, getUnitName() + "/" + Helper.getAgentName(arrived_agent)));
                    String homeServer = arrived_agent.substring(0, arrived_agent.indexOf("/", 7));
                    addPath(arrived_agent);
                    Map<String, String> data = new HashMap<>();
                    data.put("lastLocation", getUnitName() + "/" + Helper.getAgentName(arrived_agent));
                    sendMessage(homeServer + "/" + Helper.getAgentName(arrived_agent), RegionsHttpMessageFactory.createMessage("", arrived_agent, RegionsHttpMessageFactory.MessageType.AGENT_UPDATE, data));
                }
            }   
            else {
                lf("Agent [] did not change regions", arrived_agent);
                RegionsHttpAgentStatus ag = homeServerAgents.get(arrived_agent);
                if (ag.getStatus() == OFFLINE) {
                    ag.setStatus(HOME);
                    addPath(arrived_agent);
                    ag.setLastLocation(getUnitName() + "/" + Helper.getAgentName(ag.getName()));
                    while (!ag.getMessages().isEmpty()) {
                        String saved = ag.getMessages().pop();
                        li("Sending to online agent [] saved message []", arrived_agent, saved);
                        sendMessage(arrived_agent, saved);
                    }
                }
            }
            printStatus();
        }

        public void contentMessageHandler(JSONObject mesg, String message) {
            String target = (String) mesg.get("destination");
            lf("Message to send from [] to [] with content ", mesg.get("source"), target, mesg.get("content"));
            printStatus();
            RegionsHttpAgentStatus ag = homeServerAgents.get(Helper.extractAgentAddress(target));
            RegionsHttpAgentStatus agm = mobileAgents.get(Helper.extractAgentAddress(target));
            if(ag != null) {
                switch(ag.getStatus()) {
                    case HOME:
                        lf("Send message [] directly to []", mesg.get("content"), target);
                        pylon.receiveMessage(mesg);
                        break;
                    case OFFLINE:
                        lf("Saved message [] for []", mesg.get("content"), target);
                        ag.addMessage(message);
                        break;
                    case REMOTE:
                        String lastServer = ag.getLastLocation();
                        lf("Send message [] to agent [] located on []", mesg.get("content"), target, lastServer);
                        sendMessage(lastServer, message);
                        break;
                    default:
                        // can't reach here
                }
            }
            else {
                if (agm != null) {
                    lf("Send message [] directly to guest agent []", mesg.get("content"), target);
                    pylon.receiveMessage(mesg);
                }
                else {
                    String regServer = Helper.extractAgentAddress(target);
                    lf("Agent [] location isn't known. Sending message [] to home Region Server []", target,
                            mesg.get("content"), regServer);
                    sendMessage(regServer, message);
                }
            }
        }

        public void reqLeaveMessageHandler(JSONObject mesg) {
            String source = (String) mesg.get("source");
            lf("Request to leave from agent []", source);
            RegionsHttpAgentStatus ag = homeServerAgents.get(source);
            if (ag != null && ag.getStatus().equals(HOME)) {
                ag.setStatus(OFFLINE);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("source", getName());
                jsonObject.put("destination", ag.getName());
                jsonObject.put("type", REQ_ACCEPT.name());
                pylon.receiveMessage(jsonObject);
            }
            else if (mobileAgents.containsKey(source)) {
                String homeServer = source.substring(0, source.indexOf("/", 7));
                Map<String, String> data = new HashMap<>();
                data.put("agentName", source);
                sendMessage(homeServer + "/" + Helper.getAgentName(source), RegionsHttpMessageFactory.createMessage("", getName(), REQ_BUFFER, data));
            }
        }

        public void reqBufferMessageHandler(JSONObject mesg) {
            String agentReq = (String) mesg.get("agentName");
            lf("Request to buffer for agent []", agentReq);
            RegionsHttpAgentStatus ag = homeServerAgents.get(agentReq);
            if(ag != null) {
                ag.setStatus(OFFLINE);
                Map<String, String> data = new HashMap<>();
                data.put("agentName", agentReq);
                String lastLocation = ag.getLastLocation();
                sendMessage(lastLocation, RegionsHttpMessageFactory.createMessage("", getName(), REQ_ACCEPT, data));
            }
        }

        public void reqAcceptMessageHandler(JSONObject mesg) {
            String agentResp = (String) mesg.get("agentName");
            lf("Accept request received from agent []", agentResp);
            RegionsHttpAgentStatus agm = mobileAgents.get(agentResp);
            if(agm != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("source", getName());
                jsonObject.put("destination", agm.getName());
                jsonObject.put("type", REQ_ACCEPT.name());
                pylon.receiveMessage(jsonObject);
                removePath(agentResp);
            }
        }

        public void agentUpdateMessageHandler(JSONObject mesg) {
            String movedAgent = (String) mesg.get("source");
            String new_location = (String) mesg.get("lastLocation");
            RegionsHttpAgentStatus ag = homeServerAgents.get(movedAgent);
            if(ag != null) {
                lf("Agent [] arrived in []. It has [] saved messages.", movedAgent, new_location,
                        ag.getMessages().size());
                ag.setStatus(RegionsHttpAgentStatus.Status.REMOTE);
                ag.setLastLocation(new_location);
                while (!ag.getMessages().isEmpty()) {
                    String saved = ag.getMessages().pop();
                    lf("Sending to remote agent [] saved message []", movedAgent, saved);
                    sendMessage(new_location, saved);
                }
            }
            else
                lf("Agent [] arrived in [].", movedAgent, new_location);
        }

        public void agentContentMessageHandler(JSONObject mesg, String message) {
            String target = Helper.extractAgentAddress((String) mesg.get("destination"));
            String source = (String) mesg.get("source");
            lf("Agent content to send from [] to []", source, target);
            RegionsHttpAgentStatus ag = homeServerAgents.get(target);
            
            if (ag != null) {
                pylon.receiveMessage(mesg);
            }
            else {
                le("Node [] location isn't known. Sending message server []", target, target);
                sendMessage(target, message);
            }
        }
    }

    /**
     * Process messages and redirects them to the correct destination.
     *
     * @param message
     */
    public void processMessage(String message) {
        Object obj = JSONValue.parse(message);
        if(obj == null) {
            return;
        }
        JSONObject mesg = (JSONObject) obj;
        String str = (String) mesg.get("type");
        RegionsHttpServerEntity.MessageHandler handler = new MessageHandler();
        switch (RegionsHttpMessageFactory.MessageType.valueOf(str)) {
            case REGISTER:
                handler.registerMessageHandler(mesg);
                break;
            case CONNECT:
                handler.connectMessageHandler(mesg);
                break;
            case CONTENT:
                handler.contentMessageHandler(mesg, message);
                break;
            case REQ_LEAVE:
                handler.reqLeaveMessageHandler(mesg);
                break;
            case REQ_BUFFER:
                handler.reqBufferMessageHandler(mesg);
                break;
            case REQ_ACCEPT:
                handler.reqAcceptMessageHandler(mesg);
                break;
            case AGENT_UPDATE:
                handler.agentUpdateMessageHandler(mesg);
                break;
            case AGENT_CONTENT:
                handler.agentContentMessageHandler(mesg, message);
                break;
            default:
                le("Unknown type");
        }
    }
    class CustomHttpHandler extends Unit implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET"))
            {
                displayInfo(exchange);
            } else {
				RegionsHttpServerEntity.this.li("Received request on URL []", exchange.getRequestURI().getPath());
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
                
                processMessage(jsonObject.toJSONString());
            }
            catch (Exception e)
            {
                pylon.le("error when decoding the message", e);
            }
        }
    }

    /**
     * Creates a Http server instance. It must be started with {@link #start()}.
     * @param pylon the pylon reference
     * @param isHttps if server should be configured with TLS
     * @param certificatePath certificate of the HTTPS server
     */
    public RegionsHttpServerEntity(RegionsHttpPylon pylon, boolean isHttps, String certificatePath, HttpClient httpClient) {
        this.httpClient = httpClient;
        setUnitName("http-server-" + pylon.getName().split(" ")[0]);
        this.pylon = pylon;
        this.isHttps = isHttps;
        pylon.li(format("Starting {0} server on port: {1}", isHttps ? "https" : "http", pylon.getServerPort()));
        if (isHttps) {
            createHttpsServer(certificatePath);
        } else {
            createHttpServer();
        }
    }
    
    public void register(String resource) {
        if (!homeServerAgents.containsKey(resource) && resource.startsWith(getUnitName())) {
            homeServerAgents.put(resource, new RegionsHttpAgentStatus(resource, HOME, getUnitName() + "/" + Helper.getAgentName(resource)));
            addPath(resource);
        }
    }
    
    public void addPath(String resource) {
        String agentName = resource.substring(resource.indexOf("/", 7) + 1);
        if (agentName.contains("/")) {
            agentName = agentName.substring(0, agentName.indexOf("/"));
        }
        String path = "/" + agentName;
        if (isHttps) {
            httpsServer.createContext(path, new CustomHttpHandler());
        } else {
            httpServer.createContext(path, new CustomHttpHandler());
        }
        li("Added path []", path);
    }
    
    public void removePath(String resource) {
        String agentName = resource.substring(resource.indexOf("/", 7) + 1);
        if (agentName.contains("/")) {
            agentName = agentName.substring(0, agentName.indexOf("/"));
        }
        agentName = "/" + agentName;
        try {
            if (this.isHttps) {
                this.httpsServer.removeContext(agentName);
            } else {
                this.httpServer.removeContext(agentName);
            }
            li("Removed path []", agentName);
            mobileAgents.remove(resource);
        } catch (Exception e) {
            le("Cannot remove path []. Cause: []", agentName, e.getMessage());
        }
    }

    public void markAsGone(String resource) {
        if (homeServerAgents.containsKey(resource)) {
            // do not remove because this is the home server for this agent
        } else if (mobileAgents.containsKey(resource)) {
            removePath(resource);
        }
    }
    
    private void createHttpServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(pylon.getServerPort()), 0);
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
        return getUnitName();
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

