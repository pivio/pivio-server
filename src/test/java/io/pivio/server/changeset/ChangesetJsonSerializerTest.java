package io.pivio.server.changeset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangesetJsonSerializerTest {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    SimpleModule changesetModule = new SimpleModule("Changeset Module");
    changesetModule.addSerializer(Changeset.class, new ChangesetJsonSerializer());
    objectMapper.registerModule(changesetModule);
  }

  @Test
  public void shouldSerializeDocumentId() throws JsonProcessingException {
    Changeset changeset = new Changeset("randomId", 1L, Collections.emptyMap());
    JsonNode serializedJson = objectMapper.valueToTree(changeset);
    assertThat(serializedJson.get("document").textValue()).isEqualTo("randomId");
  }

  @Test
  public void shouldSerializeOrderOfChangeset() throws JsonProcessingException {
    Changeset changeset = new Changeset("randomId", 12L, Collections.emptyMap());
    JsonNode serializedJson = objectMapper.valueToTree(changeset);
    assertThat(serializedJson.get("order").longValue()).isEqualTo(12L);
  }

  @Test
  public void shouldSerializeTimestamp() throws JsonProcessingException {
    Changeset changeset = new Changeset("randomId", 1L, Collections.emptyMap());
    JsonNode serializedJson = objectMapper.valueToTree(changeset);
    assertThat(serializedJson.get("timestamp").textValue()).isNotEmpty();
  }

  @Test
  public void shouldSerializeTimestampInISO8601Format() throws JsonProcessingException, ParseException {
    Changeset changeset = new Changeset("randomId", 1L, Collections.emptyMap());
    JsonNode serializedJson = objectMapper.valueToTree(changeset);
    DateTime parsed = ISODateTimeFormat.dateTime().parseDateTime(serializedJson.get("timestamp").textValue());
    assertThat(changeset.getTimestamp()).isEqualTo(parsed);
  }

  @Test
  public void shouldAllChangedFields() throws JsonProcessingException {
    Map<String, Pair<String, String>> changedFields = new TreeMap<>(); // TreeMap to keep sorted order for easier assertions
    changedFields.put("name", Pair.of("Micro", "Macro"));
    changedFields.put("team", Pair.of("Lambda", "User"));
    Changeset changeset = new Changeset("randomId", 1L, changedFields);

    JsonNode serializedChangedFields = objectMapper.valueToTree(changeset).get("fields");
    assertThat(serializedChangedFields.isArray()).isTrue();

    JsonNode changedNameField = serializedChangedFields.get(0);
    assertThat(changedNameField.get("field").textValue()).isEqualTo("name");
    assertThat(changedNameField.get("current").textValue()).isEqualTo("Macro");
    assertThat(changedNameField.get("previous").textValue()).isEqualTo("Micro");

    JsonNode changedTeamField = serializedChangedFields.get(1);
    assertThat(changedTeamField.get("field").textValue()).isEqualTo("team");
    assertThat(changedTeamField.get("current").textValue()).isEqualTo("User");
    assertThat(changedTeamField.get("previous").textValue()).isEqualTo("Lambda");
  }
}
