package com.example.quizchatbackend.service;

import com.example.quizchatbackend.model.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class QuizService {

    private final SimpMessagingTemplate template;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public QuizService(SimpMessagingTemplate template) {
        this.template = template;
    }

    private List<Question> defaultQuestions() {
        return List.of(
            new Question(1, "Azərbaycanın paytaxtı hansıdır?",
                List.of("Gəncə", "Bakı", "Sumqayıt", "Şəki"), 1, 15),
            new Question(2, "Java hansı şirkət tərəfindən yaradılıb?",
                List.of("Microsoft", "Sun Microsystems", "Google", "IBM"), 1, 15),
            new Question(3, "HTTP hansı default portda işləyir?",
                List.of("21", "80", "443", "8080"), 1, 15),
            new Question(4, "WebSocket hansı protokoldan istifadə edir?",
                List.of("HTTP", "FTP", "WS/WSS", "TCP"), 2, 15),
            new Question(5, "Spring Boot hansı dil üçün framework-dür?",
                List.of("Python", "Java", "C#", "Go"), 1, 15)
        );
    }

    public GameRoom createRoom(String roomId, String host) {
        GameRoom room = new GameRoom();
        room.setRoomId(roomId);
        room.setHostUsername(host);
        room.setQuestions(defaultQuestions());
        room.getPlayers().add(new Player(host));
        rooms.put(roomId, room);
        return room;
    }

    public synchronized GameRoom joinRoom(String roomId, String username) {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Otaq tapılmadı: " + roomId);
        if (room.getPlayers().size() >= 2) throw new IllegalStateException("Otaq doludur");
        if (room.getPlayers().stream().noneMatch(p -> p.getUsername().equals(username))) {
            room.getPlayers().add(new Player(username));
        }
        broadcast(room, "WAITING", "Oyunçu qoşuldu: " + username);
        if (room.getPlayers().size() == 2 && room.getCurrentQuestionIndex() == -1) {
            startGame(roomId);
        }
        return room;
    }

    public synchronized void startGame(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;
        room.setCurrentQuestionIndex(-1);
        room.setFinished(false);
        room.getPlayers().forEach(p -> {
            p.setScore(0);
            p.setCorrectAnswers(0);
            p.setTotalResponseTimeMs(0);
        });
        nextQuestion(roomId);
    }

    private synchronized void nextQuestion(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        room.setCurrentQuestionIndex(room.getCurrentQuestionIndex() + 1);
        room.getAnsweredThisRound().clear();

        if (room.getCurrentQuestionIndex() >= room.getQuestions().size()) {
            finishGame(room);
            return;
        }

        Question q = room.getQuestions().get(room.getCurrentQuestionIndex());
        room.setQuestionStartTime(System.currentTimeMillis());

        Question safe = new Question(q.getId(), q.getText(), q.getOptions(), -1, q.getTimeLimitSeconds());
        broadcastQ(room, safe, "Sual " + (room.getCurrentQuestionIndex() + 1), "QUESTION");

        int idx = room.getCurrentQuestionIndex();
        int limit = q.getTimeLimitSeconds();
        scheduler.schedule(() -> {
            GameRoom r = rooms.get(roomId);
            if (r != null && r.getCurrentQuestionIndex() == idx && !r.isFinished()) {
                nextQuestion(roomId);
            }
        }, limit, TimeUnit.SECONDS);
    }

    public synchronized void submitAnswer(AnswerMessage ans) {
        GameRoom room = rooms.get(ans.getRoomId());
        if (room == null || room.isFinished()) return;
        if (room.getAnsweredThisRound().contains(ans.getUsername())) return;

        Question q = room.getQuestions().get(room.getCurrentQuestionIndex());
        if (q.getId() != ans.getQuestionId()) return;

        Player player = room.getPlayers().stream()
                .filter(p -> p.getUsername().equals(ans.getUsername()))
                .findFirst().orElse(null);
        if (player == null) return;

        room.getAnsweredThisRound().add(ans.getUsername());

        if (ans.getSelectedIndex() == q.getCorrectIndex()) {
            long rt = Math.max(0, ans.getResponseTimeMs());
            int speedBonus = Math.max(100, 1000 - (int)(rt / 100));
            player.setScore(player.getScore() + speedBonus);
            player.setCorrectAnswers(player.getCorrectAnswers() + 1);
            player.setTotalResponseTimeMs(player.getTotalResponseTimeMs() + rt);
        }

        if (room.getAnsweredThisRound().size() >= room.getPlayers().size()) {
            broadcast(room, "SCORE_UPDATE", "Hamı cavab verdi");
            scheduler.schedule(() -> nextQuestion(room.getRoomId()), 1500, TimeUnit.MILLISECONDS);
        } else {
            broadcast(room, "SCORE_UPDATE", ans.getUsername() + " cavab verdi");
        }
    }

    private void finishGame(GameRoom room) {
        room.setFinished(true);
        room.getPlayers().sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        String winner = room.getPlayers().isEmpty() ? "-" : room.getPlayers().get(0).getUsername();
        GameStateMessage state = new GameStateMessage(
            "GAME_OVER", "Oyun bitdi! Qalib: " + winner, null,
            room.getQuestions().size(), room.getQuestions().size(),
            room.getPlayers(), System.currentTimeMillis()
        );
        template.convertAndSend("/game/" + room.getRoomId(), state);
    }

    private void broadcast(GameRoom room, String type, String message) {
        Question cur = (room.getCurrentQuestionIndex() >= 0
                && room.getCurrentQuestionIndex() < room.getQuestions().size())
                ? room.getQuestions().get(room.getCurrentQuestionIndex()) : null;
        if (cur != null) {
            cur = new Question(cur.getId(), cur.getText(), cur.getOptions(), -1, cur.getTimeLimitSeconds());
        }
        broadcastQ(room, cur, message, type);
    }

    private void broadcastQ(GameRoom room, Question q, String msg, String type) {
        GameStateMessage state = new GameStateMessage(
            type, msg, q, room.getCurrentQuestionIndex() + 1,
            room.getQuestions().size(), room.getPlayers(), System.currentTimeMillis()
        );
        template.convertAndSend("/game/" + room.getRoomId(), state);
    }
}