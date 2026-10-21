package com.example.llmservice;

import java.util.List;

import com.example.llmservice.model.ModelCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "app.llm.allowed-models=qwen2.5:7b-instruct,llama3.1:8b-instruct",
        "app.llm.refresh-interval=60000",
        "llm.ollama.base-url=http://localhost:1"
})
class LlmServiceApplicationTests {

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private OllamaProperties ollamaProperties;

    @Autowired
    private ModelCatalog modelCatalog;

    @Autowired
    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        when(ollamaClient.listModels()).thenReturn(Mono.just(List.of("qwen2.5:7b-instruct", "llama3.1:8b-instruct")));
    }

    @Test
    void contextLoads() {
        assertThat(llmProperties.model()).isEqualTo("qwen2.5:7b-instruct");
        assertThat(llmProperties.allowedModels()).containsExactly("qwen2.5:7b-instruct", "llama3.1:8b-instruct");
        assertThat(ollamaProperties.baseUrl()).isEqualTo("http://localhost:1");
        // Таймауты должны биндиться из application.yml (иначе OllamaClient падает на null)
        assertThat(ollamaProperties.connectTimeout()).isEqualTo(java.time.Duration.ofSeconds(5));
        assertThat(ollamaProperties.chatTimeout()).isEqualTo(java.time.Duration.ofSeconds(30));
        assertThat(ollamaProperties.tagsTimeout()).isEqualTo(java.time.Duration.ofSeconds(3));
        assertThat(llmProperties.refreshInterval()).isEqualTo(java.time.Duration.ofSeconds(60));
        assertThat(modelCatalog).isNotNull();
    }

    @Test
    void llmPropertiesIsAllowedWorks() {
        assertThat(llmProperties.isAllowed("qwen2.5:7b-instruct")).isTrue();
        assertThat(llmProperties.isAllowed("unknown-model")).isFalse();
        assertThat(llmProperties.isAllowed(null)).isFalse();
    }

    @TestConfiguration
    static class OllamaClientConfig {
        @Bean
        public OllamaClient ollamaClient() {
            OllamaClient mock = mock(OllamaClient.class);
            when(mock.listModels()).thenReturn(Mono.just(List.of()));
            return mock;
        }
    }
}
