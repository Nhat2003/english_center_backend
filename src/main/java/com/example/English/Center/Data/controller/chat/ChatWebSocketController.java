package com.example.English.Center.Data.controller.chat;

import com.example.English.Center.Data.dto.chat.ChatMessageDTO;
import com.example.English.Center.Data.service.chat.ChatService;
import com.example.English.Center.Data.service.chat.ChatAuthorizationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

@Controller
public class ChatWebSocketController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatAuthorizationService chatAuth;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate, ChatAuthorizationService chatAuth) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.chatAuth = chatAuth;
    }

    // Client sends to /app/chat.sendMessage with a ChatMessageDTO (senderId optional, we can override)
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO incoming, StompHeaderAccessor headerAccessor) {
        // determine sender from Principal if available
        try {
            String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;
            Long senderId = incoming.getSenderId();
            if (principalName != null) {
                try { senderId = Long.parseLong(principalName); } catch (Exception ignored) {}
            }
            Long receiverId = incoming.getReceiverId();
            if (senderId == null || receiverId == null) return; // ignore

            // authorization: only allow if both are in the same class
            if (!chatAuth.inSameClass(senderId, receiverId)) {
                // optionally notify sender with an error message
                try {
                    messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", "ERROR: Not allowed to message this user (not in same class)");
                } catch (Exception ignored) {}
                return;
            }

            ChatMessageDTO saved = chatService.saveMessage(senderId, receiverId, incoming.getContent());
            // notify receiver on their personal queue
            messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", saved);
            // also send to sender (echo) so client can append message with id
            messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", saved);
            // send to conversation topic for any multi-subscribers
            messagingTemplate.convertAndSend("/topic/conversation." + saved.getConversationId(), saved);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
