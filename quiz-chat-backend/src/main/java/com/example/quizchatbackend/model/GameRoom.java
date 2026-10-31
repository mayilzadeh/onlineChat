package com.example.quizchatbackend.model;

import lombok.*;
import java.util.*;

@Data
public class GameRoom {
    private String roomId;
    private String hostUsername;
    private List<Player> players = new ArrayList<>();
    private List<Question> questions;
    private int currentQuestionIndex = -1;
    private long questionStartTime;
    private Set<String> answeredThisRound = new HashSet<>();
    private boolean finished = false;
}