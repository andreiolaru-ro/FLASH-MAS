package interfaceGenerator.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import interfaceGenerator.Element;
import interfaceGenerator.PageBuilder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Runner extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions()
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
                    vertx.eventBus().consumer("client-to-server").handler(objectMessage -> {
                        if (objectMessage.body().equals("init")) {
                            vertx.eventBus().send("server-to-client", "yes");
                        } else if (objectMessage.body().equals("stop")) {
                            vertx.close();
                        } else if (((String) objectMessage.body()).split(" ")[0].equals("button-id:")) {
                            // receiving button id from client, in order to check the port
                            // for active input and identify the id of form / spinner
                            String buttonId = ((String) objectMessage.body()).split(" ")[1];
                            var port = Element.identifyActivePortOfElement(buttonId);
                            if (port != null) {
                                var ids = Element.findActiveInputIdsFromPort(port);
                                Map<String, List<String>> data = new HashMap<>();
                                data.put("data", ids);

                                GsonBuilder gsonMapBuilder = new GsonBuilder();
                                Gson gsonObject = gsonMapBuilder.create();

                                String JSONObject = gsonObject.toJson(data);
                                // System.out.println(JSONObject);
                                vertx.eventBus().send("server-to-client", JSONObject);
                            }
                        } else if (((String) objectMessage.body()).split(" ")[0].equals("active-value:")) {
                            // receiving active input from client
                            String input = ((String) objectMessage.body()).substring("active-value: ".length());
                            // System.out.println(input);

                            Gson gson = new Gson();
                            Type type = new TypeToken<HashMap<String, List<String>>>() {
                            }.getType();
                            HashMap<String, List<String>> clonedMap = gson.fromJson(input, type);

                            // System.out.println(clonedMap);
                            var ids = clonedMap.get("data");
                            PageBuilder.guiShard.getActiveInput(ids);
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
        vertx.createHttpServer().requestHandler(router).listen(8080, http -> {
            if (http.succeeded())
                System.out.println("HTTP server started on port 8080");
            else
                System.out.println("HTTP server failed to start on port 8080");
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        System.out.println("HTTP server stopped");
    }
}
