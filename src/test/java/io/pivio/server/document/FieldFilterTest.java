package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

public class FieldFilterTest {

    @Ignore
    @Test
    public void testFilterFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        FieldFilter fieldFilter = new FieldFilter(mapper);

        JsonNode document = mapper.readTree("" +
                "{" +
                "  \"vcsroot\": \"git@github.com:pivio/pivio-client.git\"," +
                "  \"documentversion\": \"1\"," +
                "  \"contact\": \"Oliver Wehrens\"," +
                "  \"name\": \"Pivio Client\"," +
                "  \"description\": \"Reads descriptions out of source repositories and extracts information to send it to the pivio server.\"," +
                "  \"links\": {" +
                "    \"homepage\": \"http://none\"," +
                "    \"buildchain\": \"http://ci.local\"" +
                "  }," +
                "  \"demo\": {" +
                "    \"sub\": {" +
                "      \"subsub\": \"yes\"" +
                "    }" +
                "  }," +
                "  \"demo2\": {" +
                "    \"sub2\": {" +
                "      \"subsub2\": [" +
                "        {" +
                "          \"name\": \"one\"," +
                "          \"demo\": \"one\"" +
                "        }," +
                "        {" +
                "          \"name\": \"two\"," +
                "          \"demo\": \"two\"" +
                "        }" +
                "      ]" +
                "    }" +
                "  }," +
                "  \"owner\": \"Pivio\"," +
                "  \"id\": \"349534957349857387534\"," +
                "  \"type\": \"Tool\"" +
                "}" +
                "");

        ArrayList<String> outputFields = new ArrayList<>();
        outputFields.add("links.homepage");
        outputFields.add("name");
        outputFields.add("demo.sub.subsub");

        JsonNode jsonNode = fieldFilter.filterFields(document, outputFields);

        assertThat(jsonNode.toString()).isEqualTo("{\"name\":\"Pivio Client\",\"links\":{\"homepage\":\"http://none\"},\"demo\":{\"sub\":{\"subsub\":\"yes\"}}}");
    }
}