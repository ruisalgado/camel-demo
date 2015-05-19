package com.demo;

import com.demo.data.EmbeddedElasticServer;
import com.demo.route.AlertsRouteBuilder;
import com.demo.route.HealthCheckRouteBuilder;
import com.demo.route.StatsRouteBuilder;
import org.apache.camel.main.Main;

public class HealthCheckApp {

    public static void main(String... args) throws Exception {
        EmbeddedElasticServer elasticServer = new EmbeddedElasticServer();

        Main main = new Main();
        main.enableHangupSupport();
        main.addRouteBuilder(new HealthCheckRouteBuilder());
        main.addRouteBuilder(new AlertsRouteBuilder());
        main.addRouteBuilder(new StatsRouteBuilder());
        main.run(args);

        elasticServer.shutdown();
    }
}

