package io.pivio.server;

import org.assertj.core.util.Files;
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
        buildMainSourcesWhenRunningTestsWithoutGradle();
        DockerComposeContainer dockerEnvironment = startPivioServerAndElasticsearchDockerContainers();
        setSpringDataElasticsearchClusterNodesProperty(applicationContext, dockerEnvironment);
    }

    private void buildMainSourcesWhenRunningTestsWithoutGradle() {
        if (runningWithoutGradle()) {
            try {
                new ProcessBuilder("./gradlew", "build", "-x", "test")
                        .directory(Files.currentFolder())
                        .inheritIO()
                        .start()
                        .waitFor();
            }
            catch (Exception e) {
                throw new RuntimeException("could not build main sources prior to running tests", e);
            }
        }
    }

    private boolean runningWithoutGradle() {
        // Property is set in build.gradle for all Test tasks
        return System.getProperty("gradleIsRunning") == null;
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
