package io.pivio.server;

import io.pivio.server.changeset.Changeset;
import io.pivio.server.document.PivioDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

public final class TestHelper {

    public static void cleanElasticsearch(ElasticsearchTemplate elasticsearchTemplate) {
        elasticsearchTemplate.deleteIndex(PivioDocument.class);
        elasticsearchTemplate.deleteIndex(Changeset.class);

        elasticsearchTemplate.createIndex(PivioDocument.class);
        elasticsearchTemplate.putMapping(PivioDocument.class);

        elasticsearchTemplate.createIndex(Changeset.class);
        elasticsearchTemplate.putMapping(Changeset.class);

        elasticsearchTemplate.refresh(PivioDocument.class, true);
        elasticsearchTemplate.refresh(Changeset.class, true);
    }
}
