package com.example.backend.service;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Пост-фильтр ответа LLM (B5 из бэклога).
 * <p>Гарантирует, что в интерфейс попадёт осмысленный короткий текст на русском:
 * <ul>
 *   <li>убирает переводы строк, лишние пробелы, markdown-разметку и обрамляющие кавычки;</li>
 *   <li>отбрасывает ответы с иероглифами/корейским/японским (наблюдалось у qwen2.5);</li>
 *   <li>требует, чтобы кириллица составляла не менее половины букв (иначе — не русский);</li>
 *   <li>не допускает английских слов, но сохраняет шахматную нотацию вида {@code b6–a5} и {@code c3×e5};</li>
 *   <li>ограничивает длину и обрезает по границе предложения.</li>
 * </ul>
 * Пустой {@link Optional} означает «ответ непригоден» — вызывающий код
 * переключается на fallback-комментарий.
 */
public final class CommentTextSanitizer {

    /** Максимальная длина комментария в символах. */
    static final int MAX_LENGTH = 400;
    /** Минимум кириллических букв, чтобы текст считался русским. */
    private static final int MIN_CYRILLIC_LETTERS = 5;

    /** Письменности, которых в русском комментарии быть не должно. */
    private static final Set<Character.UnicodeScript> REJECTED_SCRIPTS = Set.of(
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HANGUL,
            Character.UnicodeScript.BOPOMOFO
    );

    /** Блоки с восточноазиатской пунктуацией и полноширинными символами. */
    private static final Set<Character.UnicodeBlock> REJECTED_BLOCKS = Set.of(
            Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
            Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
    );

    /** Координатная нотация хода: {@code b6–a5}, {@code c3×e5}, {@code e2-d3}. */
    private static final Pattern MOVE_NOTATION = Pattern.compile(
            "(?iu)\\b[a-h][1-8](?:\\s*[-–—×x]\\s*[a-h][1-8])*\\b");

    /** Латиница вне нотации: даже отдельные английские слова недопустимы. */
    private static final Pattern LATIN_WORD = Pattern.compile("[A-Za-z]{2,}");

    /** Очистить и проверить ответ модели. */
    public Optional<String> sanitize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.replace('\r', ' ').replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        text = text.replaceFirst("^(?iu)(комментарий|ответ|реплика)\\s*:\\s*", "");
        text = stripWrappingQuotes(text);
        text = text.replaceAll("[*_`]+", "").trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        if (containsRejectedScript(text)) {
            return Optional.empty();
        }
        if (LATIN_WORD.matcher(MOVE_NOTATION.matcher(text).replaceAll(" ")).find()) {
            // Английские вставки («... активизировать Pieces on the board») — не русский текст.
            return Optional.empty();
        }
        long letters = text.codePoints().filter(Character::isLetter).count();
        long cyrillic = text.codePoints()
                .filter(codePoint -> Character.UnicodeScript.of(codePoint)
                        == Character.UnicodeScript.CYRILLIC)
                .count();
        if (cyrillic < MIN_CYRILLIC_LETTERS || cyrillic * 2 < letters) {
            return Optional.empty();
        }
        if (text.length() > MAX_LENGTH) {
            text = truncate(text);
        }
        return Optional.of(text);
    }

    /** Убрать обрамляющие кавычки: «…», "…", '…', “…” и т.п. */
    private static String stripWrappingQuotes(String text) {
        String value = text;
        boolean changed = true;
        while (changed && value.length() >= 2) {
            changed = false;
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"')
                    || (first == '\'' && last == '\'')
                    || (first == '«' && last == '»')
                    || (first == '“' && last == '”')
                    || (first == '„' && last == '“')) {
                value = value.substring(1, value.length() - 1).trim();
                changed = true;
            }
        }
        return value;
    }

    private static boolean containsRejectedScript(String text) {
        return text.codePoints().anyMatch(codePoint -> {
            if (REJECTED_SCRIPTS.contains(Character.UnicodeScript.of(codePoint))) {
                return true;
            }
            Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
            return block != null && REJECTED_BLOCKS.contains(block);
        });
    }

    /** Обрезать по последнему предложению в пределах лимита, иначе — по слову. */
    private static String truncate(String text) {
        int limit = MAX_LENGTH - 1;
        int sentenceEnd = -1;
        for (int index = 0; index < limit; index++) {
            char symbol = text.charAt(index);
            if (symbol == '.' || symbol == '!' || symbol == '?' || symbol == '…') {
                sentenceEnd = index + 1;
            }
        }
        if (sentenceEnd >= 100) {
            return text.substring(0, sentenceEnd).trim();
        }
        int space = text.lastIndexOf(' ', limit);
        String cut = text.substring(0, space > 0 ? space : limit).stripTrailing();
        return cut + "…";
    }
}
