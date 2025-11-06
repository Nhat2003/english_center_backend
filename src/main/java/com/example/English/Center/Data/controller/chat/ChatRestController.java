package com.example.English.Center.Data.controller.chat;

import com.example.English.Center.Data.dto.chat.ChatMessageDTO;
import com.example.English.Center.Data.service.chat.ChatService;
import com.example.English.Center.Data.service.chat.ChatAuthorizationService;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.entity.users.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chat")
public class ChatRestController {
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatAuthorizationService chatAuth;

    public ChatRestController(ChatService chatService, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, ChatAuthorizationService chatAuth) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.chatAuth = chatAuth;
    }

    // GET /chat/history?with=123  -> conversation with user 123 for authenticated user
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam("with") Long otherId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();
        String username = auth.getName();
        Optional<User> uo = userRepository.findByUsername(username);
        if (uo.isEmpty()) return ResponseEntity.status(401).build();
        Long me = uo.get().getId();
        List<ChatMessageDTO> msgs = chatService.getConversation(me, otherId);
        return ResponseEntity.ok(msgs);
    }

    @PostMapping("/mark-read/{id}")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        chatService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    // POST /chat/send -> persist and forward a message (useful when frontend can't use WebSocket or as fallback)
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessageDTO incoming) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();
        String username = auth.getName();
        Optional<User> uo = userRepository.findByUsername(username);
        if (uo.isEmpty()) return ResponseEntity.status(401).build();
        Long me = uo.get().getId();

        Long receiverId = incoming.getReceiverId();
        String content = incoming.getContent();
        if (receiverId == null || content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("receiverId and content are required");
        }

        // authorization: check same class
        if (!chatAuth.inSameClass(me, receiverId)) {
            return ResponseEntity.status(403).body("You are not allowed to message that user (not in same class)");
        }

        ChatMessageDTO saved = chatService.saveMessage(me, receiverId, content);

        // forward to receiver and sender via WebSocket personal queues and conversation topic
        try {
            messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", saved);
            messagingTemplate.convertAndSendToUser(me.toString(), "/queue/messages", saved);
            messagingTemplate.convertAndSend("/topic/conversation." + saved.getConversationId(), saved);
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(saved);
    }
}
