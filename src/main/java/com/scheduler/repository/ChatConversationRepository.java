package com.scheduler.repository;

import com.scheduler.model.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<ChatConversation> findByUserIdAndActiveOrderByUpdatedAtDesc(Long userId, boolean active);
}

