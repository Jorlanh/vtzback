package com.votzz.backend.repository;

import com.votzz.backend.domain.Assembly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssemblyRepository extends JpaRepository<Assembly, UUID> {
    
    // Busca pelo Enum definido na Entidade
    List<Assembly> findByStatus(Assembly.StatusAssembly status);
    
}