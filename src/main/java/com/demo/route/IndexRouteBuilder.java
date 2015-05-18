package com.demo.route;


import org.apache.camel.builder.RouteBuilder;

public class IndexRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:index")
            .marshal().json()
            .to("log:com.demo.check.HealthChecks")
            .to("elasticsearch://local?operation=INDEX&indexName=health&indexType=check");
    }
}
