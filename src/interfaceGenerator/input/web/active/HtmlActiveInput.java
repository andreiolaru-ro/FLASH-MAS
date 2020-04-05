package interfaceGenerator.input.web.active;

import interfaceGenerator.input.web.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;

public class HtmlActiveInput extends AbstractVerticle {
    public static void main(String[] args) {
        Runner.runExample(HtmlActiveInput.class);
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(vertx);
        router.route("/static/*").handler(StaticHandler.create());
        router.route("/").handler(ctx -> {
            engine.render(new JsonObject(), "templates/active-input-1.html", res -> {
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