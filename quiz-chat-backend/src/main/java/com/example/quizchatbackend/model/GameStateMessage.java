package com.example.quizchatbackend.model;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class GameStateMessage {
    private String type;
    private String message;
    private Question question;
    private int questionIndex;
    private int totalQuestions;
    private List<Player> players;
    private long serverTimestamp;
}