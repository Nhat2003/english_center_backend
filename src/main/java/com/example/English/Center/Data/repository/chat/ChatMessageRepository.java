package com.example.English.Center.Data.repository.chat;

import com.example.English.Center.Data.entity.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // List distinct conversation ids for a user ordered by latest message time (descending)
    @Query(value = "SELECT conversation_id FROM chat_messages WHERE sender_id = :userId OR receiver_id = :userId GROUP BY conversation_id ORDER BY MAX(sent_at) DESC", nativeQuery = true)
    List<String> findConversationIdsForUser(@Param("userId") Long userId);

    // find the most recent message in a conversation
    ChatMessage findTopByConversationIdOrderBySentAtDesc(String conversationId);

    // count unread messages for a receiver in a conversation
    Long countByConversationIdAndReceiverIdAndIsReadFalse(String conversationId, Long receiverId);
}
