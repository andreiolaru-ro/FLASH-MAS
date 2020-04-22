package interfaceGenerator.input.web_old.pasive;

import interfaceGenerator.input.web_old.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;

public class HtmlPasiveInput extends AbstractVerticle {
    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Runner.runExample(HtmlPasiveInput.class);
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(vertx);
        router.route("/static/*").handler(StaticHandler.create());
        router.route("/").handler(ctx -> {
            engine.render(new JsonObject(), "templates/pasive-input-1.html", res -> {
                if (res.succeeded()) {
                    ctx.response().end(res.result());
                } else {
                    ctx.fail(res.cause());
                }
            });
        });
        router.errorHandler(500, rc -> {
            System.err.println("Handling failure");
            Throwable failure = rc.failure();
            if (failure != null) {
                failure.printStackTrace();
            }
        });

        // start a HTTP web server on port 8080
        vertx.createHttpServer().requestHandler(router).listen(8080);
    }
}