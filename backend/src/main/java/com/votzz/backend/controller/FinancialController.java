package com.votzz.backend.controller;

import com.votzz.backend.domain.CondoFinancial;
import com.votzz.backend.repository.CondoFinancialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal; // Importe necessário
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
public class FinancialController {

    private final CondoFinancialRepository repository;

    @GetMapping("/balance")
    public ResponseEntity<CondoFinancial> getBalance() {
        return ResponseEntity.ok(repository.findFirstByOrderByLastUpdateDesc());
    }

    @PostMapping("/update")
    // Alterado de Double para BigDecimal para bater com a Entidade
    public ResponseEntity<CondoFinancial> updateBalance(@RequestBody BigDecimal amount, @RequestHeader("X-Simulated-User") String user) {
        CondoFinancial fin = new CondoFinancial();
        
        fin.setBalance(amount); // Agora o método setBalance aceita o BigDecimal corretamente
        fin.setLastUpdate(LocalDateTime.now());
        fin.setUpdatedBy(user);
        
        return ResponseEntity.ok(repository.save(fin));
    }
}