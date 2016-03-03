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
    SimpleModule changesetModule = new SimpleModule("Changeset Module");
    changesetModule.addSerializer(Changeset.class, new ChangesetJsonSerializer());
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(changesetModule);
    return objectMapper;
  }
}
