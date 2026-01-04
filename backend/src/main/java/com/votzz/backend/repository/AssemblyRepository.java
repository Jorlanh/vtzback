package com.votzz.backend.repository;

import com.votzz.backend.domain.Assembly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssemblyRepository extends JpaRepository<Assembly, UUID> {
    
    // Busca por String simples (Ex: "ENCERRADA", "AGENDADA")
    List<Assembly> findByStatus(String status);
    
}