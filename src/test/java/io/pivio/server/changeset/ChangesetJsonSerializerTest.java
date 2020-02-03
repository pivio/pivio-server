package io.pivio.server.changeset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@JsonTest
public class ChangesetJsonSerializerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldSerializeDocumentId() throws JsonProcessingException {
        Changeset changeset = new Changeset("randomId", 1L, objectMapper.createArrayNode());
        JsonNode serializedJson = objectMapper.valueToTree(changeset);
        assertThat(serializedJson.get("document").textValue()).isEqualTo("randomId");
    }

    @Test
    public void shouldSerializeOrderOfChangeset() throws JsonProcessingException {
        Changeset changeset = new Changeset("randomId", 12L, objectMapper.createArrayNode());
        JsonNode serializedJson = objectMapper.valueToTree(changeset);
        assertThat(serializedJson.get("order").longValue()).isEqualTo(12L);
    }

    @Test
    public void shouldSerializeTimestamp() throws JsonProcessingException {
        Changeset changeset = new Changeset("randomId", 1L, objectMapper.createArrayNode());
        JsonNode serializedJson = objectMapper.valueToTree(changeset);
        assertThat(serializedJson.get("timestamp").asText()).isNotEmpty();
    }

    @Test
    public void shouldSerializeTimestampInISO8601Format() throws JsonProcessingException, ParseException {
        Changeset changeset = new Changeset("randomId", 1L, objectMapper.createArrayNode());
        JsonNode serializedJson = objectMapper.valueToTree(changeset);
        DateTime parsed = ISODateTimeFormat.dateTime().parseDateTime(serializedJson.get("timestamp").textValue());
        assertThat(changeset.getTimestamp()).isEqualTo(parsed);
    }

    @Test
    public void shouldSerializedOnlyFieldsNotGetterMethods() throws JsonProcessingException {
        Changeset changeset = new Changeset("randomId", 1L, objectMapper.createArrayNode());
        JsonNode serializedJson = objectMapper.valueToTree(changeset);
        // todo assertThat(serializedJson.fieldNames()).containsOnly("document", "timestamp", "order", "fields");
    }

    @Test
    public void shouldSerializeAllChangedFields() throws JsonProcessingException {
        ArrayNode changed = objectMapper.createArrayNode();
        changed.add(objectMapper.createObjectNode()
                .put("op", "REPLACE")
                .put("path", "/name")
                .put("value", "0"));
        Changeset changeset = new Changeset("randomId", 1L, changed);

        JsonNode serializedChangedFields = objectMapper.valueToTree(changeset).get("fields");
        assertThat(serializedChangedFields.isArray()).isTrue();

        JsonNode changedNameField = serializedChangedFields.get(0);
        assertThat(changedNameField.get("op").textValue()).isEqualTo("REPLACE");
        assertThat(changedNameField.get("path").textValue()).isEqualTo("/name");
        assertThat(changedNameField.get("value").textValue()).isEqualTo("0");
    }

    @Test
    public void shouldSerializeArraysInChangedValueFieldProperly() throws JsonProcessingException {
        ArrayNode changed = objectMapper.createArrayNode();
        changed.add(objectMapper.createObjectNode()
                .put("op", "REPLACE")
                .put("path", "/name")
                .put("value", "[\"a\", \"b\", \"c\"]"));
        Changeset changeset = new Changeset("randomId", 1L, changed);

        JsonNode serializedChangedFields = objectMapper.valueToTree(changeset).get("fields");
        JsonNode changedNameField = serializedChangedFields.get(0);
        assertThat(changedNameField.get("value").textValue()).isEqualTo("[\"a\", \"b\", \"c\"]");
    }

    @Test
    public void shouldSerializeNestedStructuresInChangedValueFieldProperly() throws JsonProcessingException {
        ArrayNode changed = objectMapper.createArrayNode();
        changed.add(objectMapper.createObjectNode()
                .put("op", "REPLACE")
                .put("path", "/name")
                .put("value", "{ \"test\" : { \"myarray\": [\"a\", \"b\", \"c\"] } }"));
        Changeset changeset = new Changeset("randomId", 1L, changed);

        JsonNode serializedChangedFields = objectMapper.valueToTree(changeset).get("fields");
        JsonNode changedNameField = serializedChangedFields.get(0);
        assertThat(changedNameField.get("value").textValue()).isEqualTo("{ \"test\" : { \"myarray\": [\"a\", \"b\", \"c\"] } }");
    }
}
