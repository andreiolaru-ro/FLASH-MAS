package interfaceGenerator.input.web;

import io.vertx.core.Vertx;

public class Input {
    public static void main(String[] args) throws Exception {
        var web = Vertx.vertx();
        var runner = new Runner();
        web.deployVerticle(runner);
        runner.start();
    }
}
