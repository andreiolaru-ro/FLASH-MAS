package web;

import interfaceGenerator.Element;
import interfaceGenerator.PageBuilder;
import interfaceGenerator.types.PlatformType;
import interfaceGeneratorTest.BuildPageTest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;

import java.text.DateFormat;
import java.time.Instant;
import java.util.*;

import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity.CentralEntityProxy;

class ServerVerticle extends AbstractVerticle {
    protected WebEntity entity;

    public ServerVerticle(WebEntity entity) {
        this.entity = entity;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress("server-to-client")).addInboundPermitted(new PermittedOptions().setAddress("client-to-server"));
        // mount the bridge on the router
        router.mountSubRouter("/eventbus", sockJSHandler.bridge(options, be -> {
            try {
                if (be.type() == BridgeEventType.SOCKET_CREATED) {
                    System.out.println("created");
                }
                else if(be.type() == BridgeEventType.REGISTER) {
                    System.out.println("register");
                    vertx.eventBus().consumer("client-to-server").handler(objectMessage -> {
                        if(objectMessage.body().equals("init")) {
                            vertx.eventBus().send("server-to-client", entity.getSpecification());
                            vertx.eventBus().send("server-to-client", WebEntity.cep.getEntities());
                        }
                        else {
                            //JsonObject command = new JsonObject((String) objectMessage.body());
                            //Map.Entry<String, Object> entryIterator = command.iterator().next();
                            //entity.commandAgent(entryIterator.getKey(), (String) entryIterator.getValue());
                            System.out.println(objectMessage.body());
                        }
                    });
                    vertx.setPeriodic(10000l, t -> {
                        vertx.eventBus().send("server-to-client", WebEntity.cep.getEntities());
                    });
                }
                else if(be.type() == BridgeEventType.UNREGISTER) {
                    System.out.println("unregister");
                }
                else if(be.type() == BridgeEventType.SOCKET_CLOSED) {
                    System.out.println("closed");
                }
                else if(be.type() == BridgeEventType.SEND) {
                    System.out.println("client-message");
                }
                else if(be.type() == BridgeEventType.RECEIVE) {
                    System.out.println("server-message");

                }
            } catch (Exception e) {

            } finally {
                be.complete(true);
            }
        }));
        router.route().handler(StaticHandler.create("src/web").setIndexPage("page.html"));
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

public class WebEntity implements Entity<Node> {
    public static CentralEntityProxy cep;

    protected Element specification;

    protected Vertx web;

    protected boolean running = false;

    //stubs
    protected JsonObject agents = new JsonObject();
    protected static boolean generated = false;

    public WebEntity() {
        if(!generated) {
            PageBuilder.getInstance().platformType = PlatformType.WEB;
            try {
                BuildPageTest.main(new String[] {"file", "interface-files/model-page/web-page.yml"});
                generated = true;
                specification = PageBuilder.getInstance().getPage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        start();
    }

    @Override
    public boolean start() {
        if(!running) {
            VertxOptions options = new VertxOptions();
            web = Vertx.vertx(options);
            web.deployVerticle(new ServerVerticle(this));
            running = true;
        }
        return running;
    }

    @Override
    public boolean stop() {
        if(running) {
            web.close();
            running = false;
        }
        return running;
    }

    @Override
    public boolean isRunning() {
        return false;
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

    public String getSpecification() {
        JsonObject specification = new JsonObject();
        Element entities_specification = this.specification.getChildren().get(0).getChildren().get(6);
        Element interfaces_specification = this.specification.getChildren().get(1);

        specification.put("entities", entities_specification.getId());
        specification.put("interfaces", interfaces_specification.getId());

        entities_specification.getChildren().forEach(element -> {
            specification.put("entity " + element.getValue(), element.getType() + " " + element.getRole());
        });

        interfaces_specification.getChildren().forEach(element -> {
            specification.put("entity " + element.getValue(), element.getType() + " " + element.getRole());
        });

        return specification.toString();
    }

    public void commandAgent(String name, String command) {
        if(command.equals("start"))
            agents.put(name, "running");
        if(command.equals("stop"))
            agents.put(name, "stopped");
    }

}
