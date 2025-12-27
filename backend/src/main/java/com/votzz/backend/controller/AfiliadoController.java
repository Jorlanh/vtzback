package com.votzz.backend.controller;

import com.votzz.backend.service.AffiliateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/afiliado")
@RequiredArgsConstructor
public class AfiliadoController {

    private final AffiliateService affiliateService;

    @GetMapping("/{afiliadoId}/dashboard")
    public ResponseEntity<AffiliateService.DashboardDTO> getDashboard(@PathVariable UUID afiliadoId) {
        // TODO: Adicionar validação de segurança aqui (verificar se o token JWT pertence ao afiliadoId)
        return ResponseEntity.ok(affiliateService.getDashboard(afiliadoId));
    }
    
    // Endpoint auxiliar para você forçar o pagamento manualmente durante os testes
    @PostMapping("/forcar-pagamentos")
    public ResponseEntity<String> forcePayments() {
        affiliateService.processarPagamentosAutomaticos();
        return ResponseEntity.ok("Processamento de pagamentos iniciado em background.");
    }
}