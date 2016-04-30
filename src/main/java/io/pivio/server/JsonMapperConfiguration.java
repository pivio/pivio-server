package io.pivio.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.pivio.server.changeset.Changeset;
import io.pivio.server.changeset.ChangesetJsonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonMapperConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule changesetModule = new SimpleModule("Changeset Module");
        changesetModule.addSerializer(Changeset.class, new ChangesetJsonSerializer());
        objectMapper.registerModule(changesetModule);
        return objectMapper;
    }
}
