package com.example.backend.dto;

import com.example.backend.model.Item;

import java.time.Instant;

public record ItemDto(
        Long id,
        String title,
        Instant createdAt
) {
    public static ItemDto from(Item item) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getCreatedAt()
        );
    }
}