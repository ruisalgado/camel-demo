package com.demo.route;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

public class StatsRouteBuilder extends RouteBuilder {

    private final Client esClient;

    public StatsRouteBuilder(Client esClient) {
        this.esClient = esClient;
    }

    @Override
    public void configure() throws Exception {
        restConfiguration().component("jetty").host("localhost").port(8080);

        rest("/stats").get().to("direct:stats");

        from("direct:stats")
                .setExchangePattern(ExchangePattern.InOut)
                .process(performAvailabilitySearch())
                .process(exchange -> {
                    SearchResponse search = exchange.getIn().getBody(SearchResponse.class);

                    exchange.getOut().setBody(createStatsResponse(search));
                    exchange.getOut().setHeader(Exchange.CONTENT_TYPE, "application/json");
                });
    }

    private Processor performAvailabilitySearch() throws Exception {
        SearchRequestBuilder requestBuilder = esClient.prepareSearch("health")
                .setTypes("check")
                .setSize(0)
                .setQuery(matchAllQuery())
                .addAggregation(terms("statuses").field("statusCode"));

        return exchange -> exchange.getIn().setBody(requestBuilder.execute().get());
    }

    private JSONObject createStatsResponse(SearchResponse search) throws JSONException {
        Terms statuses = search.getAggregations().get("statuses");

        long runs = search.getHits().totalHits();
        long successes = statuses.getBucketByKey("200").getDocCount();
        long timeouts = statuses.getBucketByKey("0").getDocCount();
        long errors = runs - successes - timeouts;

        JSONObject stats = new JSONObject();
        stats.put("runs", runs);
        stats.put("successes", successes);
        stats.put("timeouts", timeouts);
        stats.put("errors", errors);

        if(runs > 0)
            stats.put("availability", String.format("%.2f%%", 100 * ((double) successes / runs)));

        return stats;
    }
}
