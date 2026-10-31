package com.example.quizchatbackend.controller;

import com.example.quizchatbackend.model.AnswerMessage;
import com.example.quizchatbackend.service.QuizService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @MessageMapping("/quiz/answer")
    public void answer(AnswerMessage ans) {
        quizService.submitAnswer(ans);
    }
}