package com.alphadraft.skeleton.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class DatabaseConfigTest {

    private static final String[] REQUIRED_KEYS = {
        "spring.datasource.url",
        "spring.datasource.username",
        "spring.datasource.password",
    };

    private static Properties loadApplicationProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(in);
        }
        return properties;
    }

    private static MockEnvironment environmentWith(Properties properties) {
        MockEnvironment environment = new MockEnvironment();
        properties.forEach((key, value) -> environment.setProperty((String) key, (String) value));
        return environment;
    }

    @Test
    void missingVariablesFailFast() throws IOException {
        MockEnvironment environment = environmentWith(loadApplicationProperties());

        for (String key : REQUIRED_KEYS) {
            assertThrows(IllegalArgumentException.class, () -> environment.getRequiredProperty(key),
                    "Expected startup to fail when the variable behind " + key + " is missing");
        }
    }

    @Test
    void variablesBindToDatasourceProperties() throws IOException {
        MockEnvironment environment = environmentWith(loadApplicationProperties());
        environment.setProperty("SUPABASE_DB_URL", "jdbc:postgresql://example.test:5432/postgres");
        environment.setProperty("SUPABASE_DB_USERNAME", "postgres");
        environment.setProperty("SUPABASE_DB_PASSWORD", "secret");

        assertEquals("jdbc:postgresql://example.test:5432/postgres",
                environment.getRequiredProperty("spring.datasource.url"));
        assertEquals("postgres", environment.getRequiredProperty("spring.datasource.username"));
        assertEquals("secret", environment.getRequiredProperty("spring.datasource.password"));
    }
}
