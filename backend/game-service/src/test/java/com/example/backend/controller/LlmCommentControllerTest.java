package com.example.backend.controller;

import com.example.backend.dto.game.LikedRequest;
import com.example.backend.error.GlobalExceptionHandler;
import com.example.backend.service.LlmCommentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LlmCommentControllerTest {

    @Test
    void likeCommentSetsLikedTrueReturnsNoContent() throws Exception {
        LlmCommentService service = mock(LlmCommentService.class);
        when(service.toggleLike(any(UUID.class), eq(1L), eq(true))).thenReturn(true);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LlmCommentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/game/{id}/comments/{commentId}/like",
                        UUID.randomUUID(), 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"liked\": true}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void likeCommentSetsLikedFalseReturnsNoContent() throws Exception {
        LlmCommentService service = mock(LlmCommentService.class);
        when(service.toggleLike(any(UUID.class), eq(1L), eq(false))).thenReturn(true);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LlmCommentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/game/{id}/comments/{commentId}/like",
                        UUID.randomUUID(), 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"liked\": false}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void likeCommentNotFoundReturnsNotFound() throws Exception {
        LlmCommentService service = mock(LlmCommentService.class);
        when(service.toggleLike(any(UUID.class), any(Long.class), any(Boolean.class)))
                .thenReturn(false);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LlmCommentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/game/{id}/comments/{commentId}/like",
                        UUID.randomUUID(), 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"liked\": true}"))
                .andExpect(status().isNotFound());
    }
}
