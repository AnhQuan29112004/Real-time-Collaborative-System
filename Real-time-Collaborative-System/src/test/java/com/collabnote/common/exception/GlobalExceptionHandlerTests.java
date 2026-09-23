package com.collabnote.common.exception;

import com.collabnote.common.web.RequestIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTests {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorResponseFactory(messages)))
                .addFilters(new RequestIdFilter()).build();
    }

    @Test
    void malformedJsonAndValidationAre400WithoutEchoingInput() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void wrongMethodRetains405InsteadOfBecoming500() throws Exception {
        mvc.perform(put("/test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    void businessErrorsHaveStableCodeAndLocalizedMessage() throws Exception {
        mvc.perform(get("/test/missing").header("Accept-Language", "vi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy tài liệu."));
    }

    @Test
    void unexpectedErrorsNeverExposeExceptionMessage() throws Exception {
        mvc.perform(get("/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @RestController
    static class TestController {
        record Input(@NotBlank String name) { }
        @PostMapping("/test")
        void validate(@Valid @RequestBody Input input) { }
        @GetMapping("/test/missing")
        void missing() { throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND); }
        @GetMapping("/test/failure")
        void failure() { throw new IllegalStateException("secret-internal-value"); }
    }
}
