package florin.web;

import io.vertx.core.Vertx;

public class Input {
    public static Runner runner;

    public static void main(String[] args) throws Exception {
        Vertx web = Vertx.vertx();
        runner = new Runner();
        web.deployVerticle(runner);
        runner.start();
    }
}
