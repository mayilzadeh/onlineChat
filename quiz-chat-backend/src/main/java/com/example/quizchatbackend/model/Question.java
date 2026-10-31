package com.example.quizchatbackend.model;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class Question {
    private int id;
    private String text;
    private List<String> options;
    private int correctIndex;
    private int timeLimitSeconds;
}