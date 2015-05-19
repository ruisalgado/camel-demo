package com.demo.route;

import com.demo.check.HealthCheck;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;

import java.net.ConnectException;
import java.util.Date;
import java.util.Optional;

public class HealthCheckRouteBuilder extends RouteBuilder {

    public void configure() {
        onException(ConnectException.class)
                .process(this::toTimeout)
                .log(LoggingLevel.WARN, "processing timeout")
                .to("direct:index")
                .to("direct:alert");

        from("timer://ping?fixedRate=true&delay=0&period=10000")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.HEAD))
                .setHeader(Headers.ENDPOINT_NAME, constant("simpleHttpServer"))
                .to("http4://127.0.0.1:8000?throwExceptionOnFailure=false")
                .choice()
                    .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(200))
                        .process(this::toSuccessfulCheck)
                        .to("direct:index")
                    .otherwise()
                        .process(this::toFailedCheck)
                        .log(LoggingLevel.WARN, "processing failed check")
                        .to("direct:index")
                        .to("direct:alert");

        from("direct:index")
            .marshal().json()
            .to("log:com.demo.check.HealthChecks")
            .to("elasticsearch://local?operation=INDEX&indexName=health&indexType=check");
    }

    private void toSuccessfulCheck(Exchange exchange) {
        HealthCheck check = new HealthCheck(firedTime(exchange), statusCode(exchange));

        exchange.getIn().setBody(check);
    }

    private void toFailedCheck(Exchange exchange) {
        Date timestamp = firedTime(exchange);
        Integer status = statusCode(exchange);

        HealthCheck check = failureCause(exchange)
                .map(reason -> new HealthCheck(timestamp, status, reason))
                .orElseGet(() -> new HealthCheck(timestamp, status));

        exchange.getIn().setBody(check);
    }

    private void toTimeout(Exchange exchange) {
        HealthCheck check = new HealthCheck(firedTime(exchange), 0, "Timed out");

        exchange.getIn().setBody(check);
    }

    private Date firedTime(Exchange exchange) {
        return exchange.getIn().getHeader(Headers.TIMER_FIRED_TIME, Date.class);
    }

    private int statusCode(Exchange exchange) {
        return exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
    }

    private Optional<String> failureCause(Exchange exchange) {
        return Optional
                .ofNullable(exchange.getException())
                .map(e -> String.format("Failed with %s: %s", e.getClass().getName(), e.getMessage()));
    }
}
