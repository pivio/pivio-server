package io.pivio.server.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Profile("production")
public class ElasticsearchConnectionAvailableChecker {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConnectionAvailableChecker.class);

    private TransportClient transportClient;

    public ElasticsearchConnectionAvailableChecker(ElasticsearchTemplate elasticsearchTemplate) {
        Client client = elasticsearchTemplate.getClient();
        if (!(client instanceof TransportClient)) {
            throw new IllegalArgumentException("only Elasticsearch clients of type TransportClient are supported; client is instead of type '" + client.getClass() + "'");
        }
        transportClient = (TransportClient) client;
        log.info("TransportClient ({}) uses following transport addresses: {}", transportClient, transportClient.transportAddresses());
    }

    public boolean isConnectionToElasticsearchAvailable() {
        return checkNowThenIn5secThen10secThen20sec(this::isTransportClientConnectedToNode);
    }

    private boolean checkNowThenIn5secThen10secThen20sec(Supplier<Boolean> check) {
        for (int numberOfTriesFailed = 0; numberOfTriesFailed < 3; numberOfTriesFailed++) {
            if (check.get()) {
                return true;
            }
            int exponentialMultiplier = (int) Math.pow(2, numberOfTriesFailed);
            waitInSeconds(exponentialMultiplier * 5);
        }
        return false;
    }

    public boolean isTransportClientConnectedToNode() {
        return !transportClient.connectedNodes().isEmpty();
    }

    private void waitInSeconds(int secondsToWait) {
        log.warn("No connection to Elasticsearch available. TransportClient ({}) tries to connect during the next {}s.", transportClient, secondsToWait);
        try {
            TimeUnit.SECONDS.sleep(secondsToWait);
        }
        catch (InterruptedException e) {
            log.warn("Trying to connect to an Elasticsearch node within " + secondsToWait + "s has been interrupted. TransportClient: " + transportClient, e);
        }
    }
}
