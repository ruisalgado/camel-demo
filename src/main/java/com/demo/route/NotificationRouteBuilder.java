package com.demo.route;

import org.apache.camel.builder.RouteBuilder;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.camel.builder.script.ScriptBuilder.ruby;
import static org.apache.camel.util.toolbox.AggregationStrategies.useLatest;

public class NotificationRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        setErrorHandlerBuilder(
                deadLetterChannel("log:com.demo.route.NotificationFailure?level=ERROR")
                        .maximumRedeliveries(2)
                        .backOffMultiplier(5)
                        .useExponentialBackOff()
        );

        from("direct:notify")
                .aggregate(header(Headers.ENDPOINT_NAME), useLatest())
                    .completionSize(3)
                    .completionTimeout(MINUTES.toMillis(5))
                    .discardOnCompletionTimeout()
                .setBody(ruby("\"#{$request.headers['endpointName']} is down!\""))
                .to("smtp://localhost?to=rui@goldenloop.com");
    }
}
