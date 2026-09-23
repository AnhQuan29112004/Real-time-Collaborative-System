package com.collabnote.common.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class WebPropertiesTests {
    @Test
    void corsRejectsWildcardsPathsAndCredentialsInOrigin() {
        for (String origin : List.of("*", "https://*.example.test", "https://example.test/path",
                "https://user:password@example.test", "file:///tmp/test", "https://example.test?x=1")) {
            assertThatThrownBy(() -> new WebProperties.Cors(List.of(origin)))
                    .as(origin).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(new WebProperties.Cors(List.of("https://example.test", "http://localhost:5173"))
                .allowedOrigins()).hasSize(2);
    }
}
