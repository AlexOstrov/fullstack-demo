package com.example.backend.controller;

import com.example.backend.dto.CreateItemRequest;
import com.example.backend.dto.ItemDto;
import com.example.backend.model.Item;
import com.example.backend.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository repository;

    public ItemController(ItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ItemDto> list() {
        return repository
                .findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(ItemDto::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody CreateItemRequest request) {
        Item item = new Item(request.title());
        Item saved = repository.save(item);
        return ItemDto.from(saved);
    }
}