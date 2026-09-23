package com.collabnote;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.ApplicationContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollabNoteApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;

    @Test
    void healthIsPublicAndDoesNotExposeDatabaseDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(header().exists("X-Request-Id"));
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
    }

    @Test
    void anonymousRequestsHaveJson401AndRequestIdWithoutLoginRedirect() throws Exception {
        var response = mvc.perform(get("/api/documents").header("Accept-Language", "vi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập."))
                .andExpect(header().doesNotExist("Location"))
                .andReturn().getResponse();
        assertThat(response.getContentAsString()).contains(response.getHeader("X-Request-Id"));
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void authenticatedRequestsAreStillDeniedUntilModulePoliciesExist() throws Exception {
        mvc.perform(get("/api/documents").with(user("123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void csrfRemainsEnabledAndReturnsJsonError() throws Exception {
        mvc.perform(post("/api/documents"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void corsOnlyAcceptsConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/documents").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/documents").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void actuatorMetadataIsNotPublic() throws Exception {
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
    }
}
