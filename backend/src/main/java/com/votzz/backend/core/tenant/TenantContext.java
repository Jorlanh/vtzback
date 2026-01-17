package com.votzz.backend.core.tenant;

import java.util.UUID;

/**
 * Contexto ThreadLocal para armazenar o tenant atual durante a requisição.
 * Padrão: setTenant() + getCurrentTenant() + clear()
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    /**
     * Define o tenant para a thread atual (chamado no SecurityFilter).
     */
    public static void setTenant(UUID tenantId) {
        currentTenant.set(tenantId);
    }

    /**
     * Retorna o tenant atual da thread (ou null se não definido).
     * Este é o método que os controllers/services devem chamar.
     */
    public static UUID getCurrentTenant() { // Nome correto
        return currentTenant.get();
    }

    /**
     * Limpa o tenant da thread (obrigatório no finally do filtro para evitar vazamento).
     */
    public static void clear() {
        currentTenant.remove();
    }

    // Impede instanciação acidental
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }
}