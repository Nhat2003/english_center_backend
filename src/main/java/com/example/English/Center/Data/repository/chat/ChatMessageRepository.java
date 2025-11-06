package com.example.English.Center.Data.repository.chat;

import com.example.English.Center.Data.entity.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(String conversationId);

    List<ChatMessage> findBySenderIdAndReceiverIdOrderBySentAtAsc(Long senderId, Long receiverId);

    // convenience: find conversation between two users (either direction)
    default String makeConversationId(Long a, Long b) {
        if (a == null || b == null) return null;
        if (Long.compare(a, b) <= 0) return "user-" + a + "_user-" + b;
        return "user-" + b + "_user-" + a;
    }
}
