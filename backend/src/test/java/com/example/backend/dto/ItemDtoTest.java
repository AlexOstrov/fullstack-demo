package com.example.backend.dto;

import com.example.backend.model.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemDtoTest {

    @Test
    void fromMapsTitleAndNullValuesBeforePersist() {
        Item item = new Item("Тестовый элемент");

        ItemDto dto = ItemDto.from(item);

        assertEquals("Тестовый элемент", dto.title());
        assertNull(dto.id(), "id появляется только после persist");
        assertNull(dto.createdAt(), "createdAt проставляется в @PrePersist");
    }

    @Test
    void fromCopiesAllFieldsAsIs() {
        Item item = new Item("Другой элемент");

        ItemDto dto = ItemDto.from(item);

        assertEquals(item.getId(), dto.id());
        assertEquals(item.getTitle(), dto.title());
        assertEquals(item.getCreatedAt(), dto.createdAt());
    }
}
