package io.pivio.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.pivio.server.changeset.Changeset;
import io.pivio.server.changeset.ChangesetJsonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JsonMapperConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule changesetModule = new SimpleModule("Changeset Module");
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        changesetModule.addSerializer(Changeset.class, new ChangesetJsonSerializer());
        objectMapper.registerModule(changesetModule);
        return objectMapper;
    }
}
