package com.example.backend.service;

import com.example.backend.session.GameSession;

/** Результат {@code draw}: принял ли ИИ предложение ничьей (ТЗ 5). */
public record DrawOutcome(boolean accepted, GameSession session) {
}
