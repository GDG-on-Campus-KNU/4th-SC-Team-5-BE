package com.gdgoc5.vitaltrip.first_aid.repository;

import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmergencyChatSessionRepository extends JpaRepository<EmergencyChatSession, UUID> {
}
