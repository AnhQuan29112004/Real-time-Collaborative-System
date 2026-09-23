package com.collabnote.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class EnvFileConfigurationTests {
    @TempDir Path directory;

    @Test
    void importedEnvSelectsDevAndSuppliesDatabaseAndPort() throws Exception {
        Path file = fixture();
        try (var context = start(file, Map.of())) {
            var env = context.getEnvironment();
            assertThat(env.getActiveProfiles()).containsExactly("dev");
            assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5432/env_test");
            assertThat(env.getProperty("spring.datasource.password")).isEqualTo("fixture-only");
            assertThat(env.getProperty("server.port")).isEqualTo("8181");
        }
    }

    @Test
    void realEnvironmentOverridesFileAndSelectsProd() throws Exception {
        try (var context = start(fixture(), Map.of("SPRING_PROFILES_ACTIVE", "prod",
                "DB_PASSWORD", "override-only", "PORT", "9191"))) {
            var env = context.getEnvironment();
            assertThat(env.getActiveProfiles()).containsExactly("prod");
            assertThat(env.getProperty("spring.datasource.password")).isEqualTo("override-only");
            assertThat(env.getProperty("server.port")).isEqualTo("9191");
            assertThat(env.getProperty("spring.datasource.hikari.connection-timeout")).isEqualTo("5000");
        }
    }

    @Test
    void missingOptionalEnvAllowsDeploymentEnvironmentConfiguration() {
        try (var context = start(directory.resolve("missing.env"), Map.of(
                "SPRING_PROFILES_ACTIVE", "prod", "DB_URL", "jdbc:postgresql://db:5432/app",
                "DB_USERNAME", "app", "DB_PASSWORD", "deployment-only"))) {
            var env = context.getEnvironment();
            assertThat(env.getActiveProfiles()).containsExactly("prod");
            assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://db:5432/app");
        }
    }

    private Path fixture() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, """
                SPRING_PROFILES_ACTIVE=dev
                DB_URL=jdbc:postgresql://localhost:5432/env_test
                DB_USERNAME=fixture
                DB_PASSWORD=fixture-only
                PORT=8181
                """);
        return file;
    }

    private ConfigurableApplicationContext start(Path file, Map<String, Object> overrides) {
        var values = new HashMap<String, Object>(overrides);
        values.put("ENV_FILE", file.toString());
        var env = new StandardEnvironment();
        env.getPropertySources().addFirst(new SystemEnvironmentPropertySource("testEnvironment", values));
        var app = new SpringApplication(ConfigOnly.class);
        app.setEnvironment(env);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setRegisterShutdownHook(false);
        app.setLogStartupInfo(false);
        return app.run("--spring.main.banner-mode=off");
    }

    @Configuration(proxyBeanMethods = false)
    static class ConfigOnly { }
}
