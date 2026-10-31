package com.example.quizchatbackend.controller;

import com.example.quizchatbackend.model.GameRoom;
import com.example.quizchatbackend.service.QuizService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final QuizService quizService;

    public ApiController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/quiz/create")
    public GameRoom create(@RequestBody Map<String, String> body) {
        return quizService.createRoom(body.get("roomId"), body.get("host"));
    }

    @PostMapping("/quiz/join")
    public GameRoom join(@RequestBody Map<String, String> body) {
        return quizService.joinRoom(body.get("roomId"), body.get("username"));
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}