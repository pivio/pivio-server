package io.pivio.server.changeset;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.boot.jackson.JsonObjectSerializer;

import java.io.IOException;
import java.util.Iterator;

@JsonComponent
public class ChangesetJsonSerializer extends JsonObjectSerializer<Changeset> {

    @Override
    protected void serializeObject(Changeset changeset, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStringField("document", changeset.getDocument());
        jgen.writeNumberField("order", changeset.getOrder());
        jgen.writeStringField("timestamp", ISODateTimeFormat.dateTime().print(changeset.getTimestamp()));
        jgen.writeArrayFieldStart("fields");
        Iterator<JsonNode> patches = changeset.getFields().elements();
        while (patches.hasNext()) {
            jgen.writeStartObject();
            JsonNode current = patches.next();
            jgen.writeStringField("op", current.get("op").textValue());
            jgen.writeStringField("path", current.get("path").textValue());
            if (current.has("value")) {
                jgen.writeStringField("value",
                        removeLeadingAndTrailingDoubleQuotes(current.get("value").toString()).replace("\\\"", "\""));
            }
            jgen.writeEndObject();
        }
        jgen.writeEndArray();
    }

    private String removeLeadingAndTrailingDoubleQuotes(String str) {
        if (str.length() == 0) {
            return str;
        }

        int start = str.charAt(0) == '"' ? 1 : 0;
        int end = str.charAt(str.length() - 1) == '"' ? str.length() - 1 : str.length();
        return str.substring(start, end);
    }
}
