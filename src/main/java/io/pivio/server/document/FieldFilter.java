package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
class FieldFilter {

    private final ObjectMapper mapper;

    public FieldFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    JsonNode filterFields(JsonNode document, List<String> fields) {
        List<String> fieldsWithSubFields = getFieldWithSubFields(fields);
        Iterator<Map.Entry<String, JsonNode>> allFields = document.fields();
        ObjectNode filteredDocument = mapper.createObjectNode();

        while (allFields.hasNext()) {
            Map.Entry<String, JsonNode> currentField = allFields.next();

            if (fields.contains(currentField.getKey())) {
                filteredDocument.set(currentField.getKey(), currentField.getValue());
            }
            if (fieldsWithSubFields.contains(currentField.getKey())) {
                ObjectNode subDocument = createSubDocumentWithItsChildrenAsTopLevelAttributes(currentField);
                JsonNode subNode = filterFields(subDocument, getSubFieldForField(currentField.getKey(), fields));
                filteredDocument.set(currentField.getKey(), subNode);
            }
        }
        return filteredDocument;
    }

    private ObjectNode createSubDocumentWithItsChildrenAsTopLevelAttributes(Map.Entry<String, JsonNode> currentField) {
        ObjectNode subDocument = mapper.createObjectNode();
        JsonNode value = currentField.getValue();
        Iterator<Map.Entry<String, JsonNode>> subIterator = value.fields();
        while (subIterator.hasNext()) {
            Map.Entry<String, JsonNode> next = subIterator.next();
            subDocument.set(next.getKey(), next.getValue());
        }
        return subDocument;
    }

    private List<String> getFieldWithSubFields(List<String> fields) {
        List<String> fieldsWithSubFields = new ArrayList<>();
        for (String field : fields) {
            if (field.contains(".")) {
                fieldsWithSubFields.add(field.split("[.]")[0]);
            }
        }
        return fieldsWithSubFields;
    }

    private List<String> getSubFieldForField(String currentKey, List<String> fields) {
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            if (field.startsWith(currentKey + ".")) {
                result.add(field.split("[.]")[1]);
            }
        }
        return result;
    }

}
