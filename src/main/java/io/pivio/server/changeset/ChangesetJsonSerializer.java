package io.pivio.server.changeset;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.Iterator;

public class ChangesetJsonSerializer extends JsonSerializer<Changeset> {

  @Override
  public void serialize(Changeset changeset, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeStartObject();
    gen.writeStringField("document", changeset.getDocument());
    gen.writeNumberField("order", changeset.getOrder());
    gen.writeStringField("timestamp", ISODateTimeFormat.dateTime().print(changeset.getTimestamp()));
    gen.writeArrayFieldStart("fields");
    Iterator<JsonNode> patches = changeset.getFields().elements();
    while (patches.hasNext()) {
      gen.writeStartObject();
      JsonNode current = patches.next();
      gen.writeStringField("op", current.get("op").textValue());
      gen.writeStringField("path", current.get("path").textValue());
      if (current.has("value")) {
        gen.writeStringField("value",
                removeLeadingAndTrailingDoubleQuotes(current.get("value").toString()).replace("\\\"", "\""));
      }
      gen.writeEndObject();
    }
    gen.writeEndArray();
    gen.writeEndObject();
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
