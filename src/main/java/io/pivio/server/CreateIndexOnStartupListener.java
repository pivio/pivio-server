package io.pivio.server;

import io.pivio.server.changeset.Changeset;
import io.pivio.server.document.PivioDocument;
import io.pivio.server.elasticsearch.ElasticsearchConnectionAvailableChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class CreateIndexOnStartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private final Logger log = LoggerFactory.getLogger(CreateIndexOnStartupListener.class);

    private final ElasticsearchTemplate elasticsearchTemplate;
    // private final ElasticsearchConnectionAvailableChecker elasticsearchConnectionAvailableChecker;

    public CreateIndexOnStartupListener(ElasticsearchTemplate elasticsearchTemplate
                                        /*, ElasticsearchConnectionAvailableChecker elasticsearchConnectionAvailableChecker*/) {
        this.elasticsearchTemplate = elasticsearchTemplate;
       // this.elasticsearchConnectionAvailableChecker = elasticsearchConnectionAvailableChecker;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // todo  if (!elasticsearchConnectionAvailableChecker.isConnectionToElasticsearchAvailable()) {
        //   todo   throw createAndLogIllegalStateException("cannot create Elasticsearch indices and mappings for PivioDocument and Changeset as no connection to Elasticsearch is available");
        //  todo }

        log.info("Creating index for documents");
        // todo elasticsearchTemplate.createIndex(PivioDocument.class);
        // todo elasticsearchTemplate.putMapping(PivioDocument.class);
        // todo elasticsearchTemplate.refresh(PivioDocument.class);

        log.info("Creating index for changesets");
        // todo elasticsearchTemplate.createIndex(Changeset.class);
        // todo elasticsearchTemplate.putMapping(Changeset.class);
        // todo elasticsearchTemplate.refresh(Changeset.class);
    }

    private RuntimeException createAndLogIllegalStateException(String message) {
        RuntimeException ise = new IllegalStateException(message);
        log.error(message, ise);
        return ise;
    }
}
