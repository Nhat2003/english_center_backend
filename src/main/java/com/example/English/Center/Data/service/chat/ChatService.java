package com.example.English.Center.Data.service.chat;

import com.example.English.Center.Data.dto.chat.ChatMessageDTO;
import com.example.English.Center.Data.entity.chat.ChatMessage;
import com.example.English.Center.Data.repository.chat.ChatMessageRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.entity.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatMessageDTO saveMessage(Long senderId, Long receiverId, String content) {
        String conv = chatMessageRepository.makeConversationId(senderId, receiverId);
        ChatMessage m = ChatMessage.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .conversationId(conv)
                .build();
        ChatMessage saved = chatMessageRepository.save(m);
        return toDTO(saved);
    }

    public List<ChatMessageDTO> getConversation(Long a, Long b) {
        String conv = chatMessageRepository.makeConversationId(a, b);
        List<ChatMessage> list = chatMessageRepository.findByConversationIdOrderBySentAtAsc(conv);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void markAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(m -> {
            m.setIsRead(true);
            chatMessageRepository.save(m);
        });
    }

    private ChatMessageDTO toDTO(ChatMessage m) {
        return ChatMessageDTO.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .receiverId(m.getReceiverId())
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .isRead(m.getIsRead())
                .conversationId(m.getConversationId())
                .build();
    }
}

