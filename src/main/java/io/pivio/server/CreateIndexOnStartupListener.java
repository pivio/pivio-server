package io.pivio.server;

import io.pivio.server.changeset.Changeset;
import io.pivio.server.document.PivioDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Component;

@Component
public class CreateIndexOnStartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private final Logger log = LoggerFactory.getLogger(CreateIndexOnStartupListener.class);

    private final ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    public CreateIndexOnStartupListener(ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Creating index for documents");
        elasticsearchTemplate.createIndex(PivioDocument.class);
        elasticsearchTemplate.putMapping(PivioDocument.class);
        elasticsearchTemplate.refresh(PivioDocument.class, true);

        log.info("Creating index for changesets");
        elasticsearchTemplate.createIndex(Changeset.class);
        elasticsearchTemplate.putMapping(Changeset.class);
        elasticsearchTemplate.refresh(Changeset.class, true);
    }
}
