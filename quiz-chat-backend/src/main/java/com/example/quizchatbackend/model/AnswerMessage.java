package com.example.quizchatbackend.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AnswerMessage {
    private String roomId;
    private String username;
    private int questionId;
    private int selectedIndex;
    private long responseTimeMs;
}