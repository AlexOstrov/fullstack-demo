package com.example.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** B5: пост-фильтр ответа LLM. */
class CommentTextSanitizerTest {

    private final CommentTextSanitizer sanitizer = new CommentTextSanitizer();

    @Test
    void acceptsRussianTextAndTrimsWhitespace() {
        assertThat(sanitizer.sanitize("  Отличный ход, дамка в игре!  "))
                .contains("Отличный ход, дамка в игре!");
    }

    @Test
    void stripsWrappingQuotesMarkdownAndPrefix() {
        assertThat(sanitizer.sanitize("Комментарий: «**Отличный ход!**»"))
                .contains("Отличный ход!");
    }

    @Test
    void rejectsChineseJapaneseKoreanCharacters() {
        assertThat(sanitizer.sanitize("Отличный ход! 但是 это не по-русски.")).isEmpty();
        assertThat(sanitizer.sanitize("Хороший ход。")).isEmpty();
        assertThat(sanitizer.sanitize("Хороший ход! 좋아요")).isEmpty();
    }

    @Test
    void rejectsTextWithoutRussian() {
        assertThat(sanitizer.sanitize("Great move!")).isEmpty();
        assertThat(sanitizer.sanitize("   ")).isEmpty();
        assertThat(sanitizer.sanitize(null)).isEmpty();
    }

    @Test
    void rejectsTextWhereRussianIsMinority() {
        assertThat(sanitizer.sanitize("Отличный move by black king!"))
                .as("латиница преобладает — это не русский комментарий")
                .isEmpty();
    }

    @Test
    void truncatesLongTextAtSentenceBoundary() {
        String longText = "Хороший ход, продолжаем партию. ".repeat(40);

        String result = sanitizer.sanitize(longText).orElseThrow();

        assertThat(result.length()).isLessThanOrEqualTo(CommentTextSanitizer.MAX_LENGTH);
        assertThat(result).endsWith(".");
        assertThat(result).doesNotContain("…");
    }

    @Test
    void truncatesWithoutSentenceByWordBoundary() {
        String longText = "оченьдлинноесловобезточек ".repeat(40);

        String result = sanitizer.sanitize(longText).orElseThrow();

        assertThat(result.length()).isLessThanOrEqualTo(CommentTextSanitizer.MAX_LENGTH);
        assertThat(result).endsWith("…");
    }
}
