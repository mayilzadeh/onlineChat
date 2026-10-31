package com.example.quizchatbackend.controller;

import com.example.quizchatbackend.model.ChatMessage;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final SimpMessagingTemplate template;

    public ChatController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/chat.private")
    public void privateMessage(ChatMessage msg) {
        msg.setTimestamp(LocalDateTime.now().toString());
        template.convertAndSend("/queue/messages/" + msg.getTo(), msg);
    }

    @MessageMapping("/chat.room/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage roomMessage(@DestinationVariable String roomId, ChatMessage msg) {
        msg.setRoomId(roomId);
        msg.setTimestamp(LocalDateTime.now().toString());
        return msg;
    }
}