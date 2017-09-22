package io.pivio.server;

import org.junit.runner.Description;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

public class DockerEnvironmentInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String ELASTICSEARCH_SERVICE_NAME = "elasticsearch_1";
    private static final int ELASTICSEARCH_SERVICE_PORT = 9300;

    private static final Description DOES_NOT_MATTER = null;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        DockerComposeContainer dockerEnvironment = startPivioServerAndElasticsearchDockerContainers();
        setSpringDataElasticsearchClusterNodesProperty(applicationContext, dockerEnvironment);
    }

    private DockerComposeContainer startPivioServerAndElasticsearchDockerContainers() {
        DockerComposeContainer dockerEnvironment = new DockerComposeContainer(new File("docker-compose.yml"))
                .withLocalCompose(true)
                .withExposedService(ELASTICSEARCH_SERVICE_NAME, ELASTICSEARCH_SERVICE_PORT) // needed for cleaning up persistent data - see TestHelper.cleanUpPersistentData(..)
                .withTailChildContainers(true);
        dockerEnvironment.starting(DOES_NOT_MATTER);
        return dockerEnvironment;
    }

    private void setSpringDataElasticsearchClusterNodesProperty(ConfigurableApplicationContext applicationContext, DockerComposeContainer dockerEnvironment) {
        Integer elasticsearchAmbassadorPort = dockerEnvironment.getServicePort(ELASTICSEARCH_SERVICE_NAME, ELASTICSEARCH_SERVICE_PORT);
        EnvironmentTestUtils.addEnvironment(applicationContext, "spring.data.elasticsearch.cluster-nodes=localhost:" + elasticsearchAmbassadorPort);
    }
}
