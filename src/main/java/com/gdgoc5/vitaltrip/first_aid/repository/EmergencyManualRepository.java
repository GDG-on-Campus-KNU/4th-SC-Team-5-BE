package com.gdgoc5.vitaltrip.first_aid.repository;

import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyManual;
import com.gdgoc5.vitaltrip.first_aid.entity.EmergencyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyManualRepository extends JpaRepository<EmergencyManual, java.util.UUID> {
    List<EmergencyManual> findByEmergencyType(EmergencyType emergencyType);
}
