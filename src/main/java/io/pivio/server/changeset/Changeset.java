package io.pivio.server.changeset;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.joda.time.DateTime;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "changeset", type = "changeset")
@Setting(settingPath = "settings.json")
@Mapping(mappingPath = "changeset-mapping.json")
public class Changeset {

    private String document;
    private long order;
    private DateTime timestamp;
    private ArrayNode fields;

    public Changeset(String document, long order, ArrayNode fields) {
        this.document = document;
        this.order = order;
        this.fields = fields;
        timestamp = DateTime.now();
    }

    public String getDocument() {
        return document;
    }

    public long getOrder() {
        return order;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public ArrayNode getFields() {
        return fields;
    }

    /**
     * For testing purpose.
     */
    protected void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isEmpty() {
        return fields.isMissingNode() || fields.size() == 0;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
