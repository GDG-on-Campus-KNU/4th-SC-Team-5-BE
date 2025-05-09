package com.gdgoc5.vitaltrip.first_aid.repository;

import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface EmergencyChatMessageRepository extends JpaRepository<EmergencyChatMessage, UUID> {
    List<EmergencyChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
