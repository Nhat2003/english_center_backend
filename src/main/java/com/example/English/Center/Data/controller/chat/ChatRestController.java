package com.example.English.Center.Data.controller.chat;

import com.example.English.Center.Data.dto.chat.ChatContactDTO;
import com.example.English.Center.Data.dto.chat.ChatConversationDTO;
import com.example.English.Center.Data.dto.chat.ChatMessageDTO;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.entity.users.UserRole;
import com.example.English.Center.Data.repository.chat.ChatMessageRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.chat.ChatAuthorizationService;
import com.example.English.Center.Data.service.chat.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatRestController {
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatAuthorizationService chatAuth;
    private final ClassEntityRepository classEntityRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatRestController(ChatService chatService,
                              UserRepository userRepository,
                              SimpMessagingTemplate messagingTemplate,
                              ChatAuthorizationService chatAuth,
                              ClassEntityRepository classEntityRepository,
                              ChatMessageRepository chatMessageRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.chatAuth = chatAuth;
        this.classEntityRepository = classEntityRepository;
        this.chatMessageRepository = chatMessageRepository;
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

        // authorization: allow if same class OR either side is ADMIN
        if (!chatAuth.canMessage(me, receiverId)) {
            return ResponseEntity.status(403).body("You are not allowed to message that user");
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

    // New API: danh sách người dùng cùng lớp để phục vụ chatbox
    // Optional classId parameter: nếu có -> search chỉ trong 1 lớp; nếu không -> search tất cả lớp
    @GetMapping("/contacts")
    public ResponseEntity<List<ChatContactDTO>> getContacts(@RequestParam(required = false) Long classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();
        Optional<User> uo = userRepository.findByUsername(auth.getName());
        if (uo.isEmpty()) return ResponseEntity.status(401).build();
        Long me = uo.get().getId();

        Set<Long> myClassIds = chatAuth.getClassIdsForUser(me);

        // If classId provided, validate user is in that class and use only that class
        if (classId != null) {
            if (!myClassIds.contains(classId)) {
                return ResponseEntity.status(403).body(List.of()); // Not in this class
            }
            myClassIds = Set.of(classId); // Use only this class
        }

        // Load classRooms only when user has classes
        List<ClassRoom> classRooms = new ArrayList<>();
        if (!myClassIds.isEmpty()) {
            classEntityRepository.findAllById(myClassIds).forEach(cr -> {
                if (cr != null) classRooms.add(cr);
            });
        }

        Map<Long, ChatContactDTO> map = new HashMap<>();
        classRooms.forEach(cr -> {
            if (cr == null) return;
            // Teacher
            if (cr.getTeacher() != null && cr.getTeacher().getUser() != null) {
                User tu = cr.getTeacher().getUser();
                if (!Objects.equals(tu.getId(), me)) {
                    map.putIfAbsent(tu.getId(), ChatContactDTO.builder()
                            .id(tu.getId())
                            .fullName(tu.getFullName())
                            .role(tu.getRole() != null ? tu.getRole().name() : null)
                            .build());
                }
            }
            // Students
            if (cr.getStudents() != null) {
                cr.getStudents().forEach(st -> {
                    if (st != null && st.getUser() != null) {
                        User su = st.getUser();
                        if (!Objects.equals(su.getId(), me)) {
                            map.putIfAbsent(su.getId(), ChatContactDTO.builder()
                                    .id(su.getId())
                                    .fullName(su.getFullName())
                                    .role(su.getRole() != null ? su.getRole().name() : null)
                                    .build());
                        }
                    }
                });
            }
        });

        // Always include active admins so everyone can message admins
        List<User> admins = userRepository.findByRoleAndIsActiveTrue(UserRole.ADMIN);
        for (User a : admins) {
            if (a == null) continue;
            if (Objects.equals(a.getId(), me)) continue; // skip self
            map.putIfAbsent(a.getId(), ChatContactDTO.builder()
                    .id(a.getId())
                    .fullName(a.getFullName())
                    .role(a.getRole() != null ? a.getRole().name() : null)
                    .build());
        }

        List<ChatContactDTO> result = map.values().stream()
                .sorted(Comparator.comparing(ChatContactDTO::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // GET /chat/conversations?page=0&size=50
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();
        Optional<User> uo = userRepository.findByUsername(auth.getName());
        if (uo.isEmpty()) return ResponseEntity.status(401).build();
        Long me = uo.get().getId();

        List<String> convIds = chatMessageRepository.findConversationIdsForUser(me);
        if (convIds == null || convIds.isEmpty()) return ResponseEntity.ok(List.of());

        // apply paging in-memory (small sizes expected)
        int from = Math.min(page * size, convIds.size());
        int to = Math.min(from + size, convIds.size());
        List<String> pageConv = convIds.subList(from, to);

        List<ChatConversationDTO> result = new ArrayList<>();
        for (String cid : pageConv) {
            com.example.English.Center.Data.entity.chat.ChatMessage last = chatMessageRepository.findTopByConversationIdOrderBySentAtDesc(cid);
            if (last == null) continue;
            // determine other user id
            String[] parts = cid.split("_");
            Long otherId = null;
            try {
                for (String p : parts) {
                    if (!p.startsWith("user-")) continue;
                    Long uid = Long.parseLong(p.substring(5));
                    if (!uid.equals(me)) { otherId = uid; break; }
                }
            } catch (Exception ignored) {}

            String otherName = null;
            if (otherId != null) {
                Optional<User> ou = userRepository.findById(otherId);
                otherName = ou.map(User::getFullName).orElse(null);
            }

            Long unread = 0L;
            if (otherId != null) {
                // count messages in this conversation where receiver is the current user and isRead=false
                unread = chatMessageRepository.countByConversationIdAndReceiverIdAndIsReadFalse(cid, me);
            }

            ChatConversationDTO dto = ChatConversationDTO.builder()
                    .conversationId(cid)
                    .otherUserId(otherId)
                    .otherUserName(otherName)
                    .lastMessage(last.getContent())
                    .lastAt(last.getSentAt())
                    .unreadCount(unread)
                    .build();
            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }
}
