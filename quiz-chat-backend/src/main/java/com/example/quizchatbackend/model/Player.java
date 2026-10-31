package com.example.quizchatbackend.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Player {
    private String username;
    private int score = 0;
    private int correctAnswers = 0;
    private long totalResponseTimeMs = 0;

    public Player(String username) {
        this.username = username;
    }
}