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
    private WebEntity entity;

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
                            vertx.eventBus().send("server-to-client", entity.cep.getEntities());
                        }
                        else {

                            System.out.println(objectMessage.body());
                            CentralEntityProxy cep = entity.cep;
                            JsonObject message = new JsonObject((String) objectMessage.body());
                            Iterator<Map.Entry<String, Object>> entryIterator = message.iterator();

                            while(entryIterator.hasNext()) {
                                Map.Entry<String, Object> entry = entryIterator.next();

                                String entity = entry.getKey();
                                JsonObject input = (JsonObject) entry.getValue();
                                if(input.getString("type").equals("operation")) {
                                    //TODO: opertaions for entities do not have parameters yet
                                    String name = input.getString("name").split(" ")[1];
                                    String[] parameters = input.getString("name").split(" ");
                                    if(entity.equals("all"))
                                        cep.sendToAllAgents(name);
                                    else
                                        cep.sendToEntity(entity, name);
                                }
                                else if(input.getString("type").equals("message")) {
                                    System.out.println("CHECK!!!");
                                    String[] content_destination = input.getString("content_destination").split(" ");
                                    cep.sendAgentMessage(content_destination[2], "From " + entity + ": " + content_destination[1]);
                                }
                                else {
                                    //TODO: needed for other input options
                                }
                            }
                        }
                    });
                    vertx.setPeriodic(10000l, t -> {
                        JsonObject entities = new JsonObject((String) WebEntity.cep.getEntities());
                        //entities.remove("AgentA");
                        vertx.eventBus().send("server-to-client", entities.toString());
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
    static CentralEntityProxy cep;

    private Element specification;

    private Vertx web;

    private boolean running = false;

    private static boolean generated = false;

    //stubs
    private JsonObject agents = new JsonObject();

    public WebEntity(CentralEntityProxy cep) {
        if(!generated) {
            PageBuilder.getInstance().platformType = PlatformType.WEB;
            try {
                this.cep = cep;
                BuildPageTest.main(new String[] {"file", "interface-files/model-page/web-page.yml"});
                specification = PageBuilder.getInstance().getPage();
                generated = true;
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
            specification.put("entity " + element.getPort() + " " + element.getRole() + " " + element.getType(), element.getValue());
        });

        interfaces_specification.getChildren().forEach(element -> {
            specification.put("interface " + element.getPort() + " " + element.getRole() + " " + element.getType(), element.getValue());
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
