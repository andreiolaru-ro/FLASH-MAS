package interfaceGenerator.input.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

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
                            vertx.eventBus().send("server-to-client", "PLM");

                        } else if (objectMessage.body().equals("stop")) {
                            vertx.close();
                        } else {
                            JsonObject command = new JsonObject((String) objectMessage.body());
                            Map.Entry<String, Object> entryIterator = command.iterator().next();
                            //entity.commandAgent(entryIterator.getKey(), (String) entryIterator.getValue());
                        }
                    });
                    vertx.setPeriodic(10000l, t -> {
                        vertx.eventBus().send("server-to-client", "plm");
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

            } finally {
                be.complete(true);
            }
        }));
        router.route()
                .handler(StaticHandler.create("src/interfaceGenerator/input/web")
                        .setIndexPage("index.html"));
        vertx.createHttpServer().requestHandler(router).listen(8080, http -> {
            if (http.succeeded())
                System.out.println("HTTP server started on port 8080");
            else
                System.out.println("HTTP server failed to start on port 8080");
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        System.out.println("HTTP server stoped");
    }
}
