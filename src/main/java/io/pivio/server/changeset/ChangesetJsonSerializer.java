package io.pivio.server.changeset;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.Map;

public class ChangesetJsonSerializer extends JsonSerializer<Changeset> {

  @Override
  public void serialize(Changeset changeset, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeStartObject();
    gen.writeStringField("document", changeset.getDocument());
    gen.writeNumberField("order", changeset.getOrder());
    gen.writeStringField("timestamp", ISODateTimeFormat.dateTime().print(changeset.getTimestamp()));
    gen.writeArrayFieldStart("fields");
    for (Map.Entry<String, Pair<String, String>> changedField : changeset.getChanged().entrySet()) {
      gen.writeStartObject();
      gen.writeStringField("field", changedField.getKey());
      gen.writeStringField("current", changedField.getValue().getRight());
      gen.writeStringField("previous", changedField.getValue().getLeft());
      gen.writeEndObject();
    }
    gen.writeEndArray();
    gen.writeEndObject();
  }
}
