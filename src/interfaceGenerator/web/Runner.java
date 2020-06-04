package interfaceGenerator.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import interfaceGenerator.PageBuilder;
import interfaceGenerator.Pair;
import interfaceGenerator.Utils;
import interfaceGenerator.io.IOShard;
import interfaceGenerator.io.IOShardWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Runner extends AbstractVerticle {
    public static Router router;
    public static SockJSHandler sockJSHandler;
    public static BridgeOptions options;
    private Gson gson = new Gson();
    private Type inputType = new TypeToken<HashMap<String, Map<String, String>>>() {
    }.getType();
    public static boolean connectionInit = false;
    private static String activePort;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        router = Router.router(vertx);
        sockJSHandler = SockJSHandler.create(vertx);
        options = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions()
                        .setAddress("server-to-client"))
                .addInboundPermitted(new PermittedOptions()
                        .setAddress("client-to-server"));
        // mount the bridge on the router
        router.mountSubRouter("/eventbus", sockJSHandler.bridge(options, be -> {
            try {
                if (be.type() == BridgeEventType.SOCKET_CREATED) {
                    System.out.println("created");
                } else if (be.type() == BridgeEventType.REGISTER) {
                    System.out.println("register");
                    connectionInit = true;
                    vertx.eventBus().consumer("client-to-server").handler(objectMessage -> {
                        if (objectMessage.body().equals("init")) {
                            vertx.eventBus().send("server-to-client", "yes");
                        } else if (objectMessage.body().equals("stop")) {
                            vertx.close();
                        } else if (((String) objectMessage.body()).split(" ")[0].equals("button-id:")) {
                            // receiving button id from client, in order to check the port
                            // for active input and identify the id of form / spinner
                            String buttonId = ((String) objectMessage.body()).split(" ")[1];
                            String port = Utils.identifyActivePortOfElement(buttonId);
                            if (port != null) {
                                activePort = port;
                                List<String> ids = Utils.findActiveInputIdsFromPort(port);
                                Map<String, List<String>> data = new HashMap<>();
                                data.put("data", ids);

                                GsonBuilder gsonMapBuilder = new GsonBuilder();
                                Gson gsonObject = gsonMapBuilder.create();

                                String JSONObject = gsonObject.toJson(data);
                                // System.out.println(JSONObject);
                                vertx.eventBus().send("server-to-client", "active-input: " + JSONObject);
                            }
                        } else if (((String) objectMessage.body()).split(" ")[0].equals("active-value:")) {
                            // receiving active input from client
                            String activeInput = ((String) objectMessage.body()).substring("active-value: ".length());
                            // System.out.println(input);

                            HashMap<String, Map<String, String>> clonedMap = gson.fromJson(activeInput, inputType);

                            // map of id - value of element with the respective id
                            Map<String, String> ids = clonedMap.get("data");
                            ArrayList<Pair<String, String>> data = new ArrayList<>();
                            for (Map.Entry<String, String> entry : ids.entrySet()) {
                                String role = Utils.findRoleOfElementById(entry.getKey());
                                data.add(new Pair<>(role, entry.getValue()));
                            }
                            try {
                                if (data.isEmpty()) {
                                    // especially for reduced interface
                                    String value = IOShard.reducedInterfacesValues.get(activePort);
                                    if (value != null) {
                                        data.add(new Pair<>("message", value));
                                    }
                                }
                                System.out.println(data);
                                if (!data.isEmpty()) {
                                    PageBuilder.getInstance().ioShard.getActiveInput(data);
                                } else {
                                    System.out.println("No active input available");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (((String) objectMessage.body()).split(" ")[0].equals("passive-value:")) {
                            String passiveInput = ((String) objectMessage.body()).substring("passive-value: ".length());
                            HashMap<String, Map<String, String>> clonedMap = gson.fromJson(passiveInput, inputType);

                            // map of id - value of element with the respective id
                            Map<String, String> ids = clonedMap.get("data");
                            ArrayList<Pair<String, String>> data = new ArrayList<>();
                            for (Map.Entry<String, String> entry : ids.entrySet()) {
                                String role = Utils.findRoleOfElementById(entry.getKey());
                                data.add(new Pair<>(role, entry.getValue()));
                            }
                            System.out.println(data);
                            IOShardWeb.sendPassiveInputToShard(data);
                        }
                    });
                } else if (be.type() == BridgeEventType.UNREGISTER) {
                    System.out.println("unregister");
                } else if (be.type() == BridgeEventType.SOCKET_CLOSED) {
                    System.out.println("closed");
                } else if (be.type() == BridgeEventType.SEND) {
                    System.out.println("client-message");
                } else if (be.type() == BridgeEventType.RECEIVE) {
                    System.out.println("server-message");

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                be.complete(true);
            }
        }));

        router.route()
                .handler(StaticHandler.create("interface-files/generated-web-pages")
                        .setIndexPage("page.html"));

        vertx.createHttpServer().requestHandler(router).listen(8081, http -> {
            if (http.succeeded())
                System.out.println("HTTP server started on port 8081");
            else
                System.out.println("HTTP server failed to start on port 8081");
        });

    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        System.out.println("HTTP server stopped");
    }
}
