package com.example.quizchatbackend.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {
    private String from;
    private String to;
    private String content;
    private String timestamp;
    private String roomId;
}