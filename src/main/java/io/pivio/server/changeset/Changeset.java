package io.pivio.server.changeset;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.util.Map;

@Document(indexName = "changeset", type = "changeset")
@Mapping(mappingPath = "changeset-mapping.json")
public class Changeset {

  private String document;
  private long order;
  private DateTime timestamp;
  private Map<String, Pair<String, String>> changed;

  public Changeset(String document, long order, Map<String, Pair<String, String>> changed) {
    this.document = document;
    this.order = order;
    this.changed = changed;
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

  /**
   * For testing purpose.
   */
  protected void setTimestamp(DateTime timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, Pair<String, String>> getChanged() {
    return changed;
  }

  public boolean isEmpty() {
    return changed.isEmpty();
  }

  public boolean isNotEmpty() {
    return !changed.isEmpty();
  }
}
