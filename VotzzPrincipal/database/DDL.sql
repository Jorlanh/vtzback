-- ====================================================================
-- 1. LIMPEZA TOTAL (RESET - CUIDADO AO RODAR EM PROD)
-- ====================================================================

-- Remove constraints que causam dependência circular antes de dropar
ALTER TABLE IF EXISTS tenants DROP CONSTRAINT IF EXISTS fk_tenants_afiliados;

-- Drop de tabelas (Ordem reversa de dependência ou usando CASCADE)
DROP TABLE IF EXISTS tenant_payment_config CASCADE; -- Configuração de Pagamento Híbrido
DROP TABLE IF EXISTS user_unidades CASCADE; -- Lista de Unidades
DROP TABLE IF EXISTS tb_orders CASCADE;
DROP TABLE IF EXISTS trusted_devices CASCADE;
DROP TABLE IF EXISTS calendar_events CASCADE;
DROP TABLE IF EXISTS poll_votes CASCADE;
DROP TABLE IF EXISTS poll_options CASCADE;
DROP TABLE IF EXISTS polls CASCADE;
DROP TABLE IF EXISTS ticket_messages CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS financial_reports CASCADE;
DROP TABLE IF EXISTS tickets CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS announcement_reads CASCADE;
DROP TABLE IF EXISTS announcements CASCADE;
DROP TABLE IF EXISTS chat_message CASCADE;
DROP TABLE IF EXISTS votes CASCADE;
DROP TABLE IF EXISTS condo_financial CASCADE;
DROP TABLE IF EXISTS common_areas CASCADE; 
DROP TABLE IF EXISTS poll_options_assembly CASCADE; 
DROP TABLE IF EXISTS assemblies CASCADE;
DROP TABLE IF EXISTS comissoes CASCADE;
DROP TABLE IF EXISTS afiliados CASCADE;
DROP TABLE IF EXISTS leads_captura CASCADE;
DROP TABLE IF EXISTS user_tenants CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE; 
DROP TABLE IF EXISTS tenants CASCADE;
DROP TABLE IF EXISTS coupons CASCADE;
DROP TABLE IF EXISTS planos CASCADE;
DROP TABLE IF EXISTS tb_guests CASCADE;

-- Habilita extensão para UUID (Necessário para gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ====================================================================
-- 2. CAMADA SAAS E CONFIGURAÇÃO
-- ====================================================================

CREATE TABLE planos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(50) NOT NULL,
    ciclo VARCHAR(20) NOT NULL, 
    max_unidades INTEGER NOT NULL,
    preco_base NUMERIC(19, 2) NOT NULL,
    taxa_servico_reserva NUMERIC(19, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_percent NUMERIC(5, 2) NOT NULL, 
    quantity INTEGER DEFAULT 9999,
    active BOOLEAN DEFAULT TRUE,
    expiration_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leads_captura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255),
    whatsapp VARCHAR(20),
    email VARCHAR(255),
    status VARCHAR(50) DEFAULT 'NOVO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- 3. ESTRUTURA CORE (Tenants e Users)
-- ====================================================================

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20) UNIQUE NOT NULL,
    
    plano_id UUID REFERENCES planos(id),
    
    asaas_customer_id VARCHAR(50), 
    asaas_wallet_id VARCHAR(50),    
    kiwify_transaction_id VARCHAR(100),
    status_assinatura VARCHAR(20) DEFAULT 'PENDING', 

    banco_nome VARCHAR(100),
    banco_agencia VARCHAR(20),
    banco_conta VARCHAR(20),
    chave_pix VARCHAR(255),

    data_expiracao_plano DATE,
    unidades_total INTEGER DEFAULT 0,
    blocos_total INTEGER DEFAULT 1,
    
    cep VARCHAR(20),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2), 
    ponto_referencia VARCHAR(255),

    afiliado_id UUID,
    
    secret_keyword VARCHAR(100),
    
    ativo BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL, 
    external_reference VARCHAR(255) UNIQUE NOT NULL, 
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    plan_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    next_billing_date TIMESTAMP,
    payload_raw TEXT, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    
    cpf VARCHAR(14),
    
    -- Unidade Principal (Legado/Compatibilidade)
    unidade VARCHAR(50),
    bloco VARCHAR(50), 
    
    whatsapp VARCHAR(20), 
    phone VARCHAR(20), -- Adicionado para compatibilidade com setPhone()
    role VARCHAR(20) NOT NULL, 
    
    enabled BOOLEAN DEFAULT TRUE, 

    -- CAMPOS 2FA
    is_2fa_enabled BOOLEAN DEFAULT FALSE,
    secret_2fa VARCHAR(255),
    
    is_2fa_enabled_backup BOOLEAN DEFAULT FALSE,
    
    last_seen TIMESTAMP,

    reset_token VARCHAR(255),
    reset_token_expiry TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- 3.1 TABELA DE LISTA DE UNIDADES (CORREÇÃO PARA @ElementCollection)
-- ====================================================================
CREATE TABLE user_unidades (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    unidade_info VARCHAR(255)
);
CREATE INDEX idx_user_unidades_user ON user_unidades(user_id);

-- Tabela de Junção para Multi-Tenancy
CREATE TABLE user_tenants (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, tenant_id)
);

-- ====================================================================
-- AJUSTE CRÍTICO DE ÍNDICES PARA MULTI-UNIDADE
-- ====================================================================

-- 1. Permite mesmo email no mesmo condomínio SE a unidade/bloco for diferente
CREATE UNIQUE INDEX idx_users_email_tenant_unit ON users (email, tenant_id, unidade, bloco) WHERE tenant_id IS NOT NULL;

-- 2. Permite mesmo CPF no mesmo condomínio SE a unidade/bloco for diferente
CREATE UNIQUE INDEX idx_users_cpf_tenant_unit ON users (cpf, tenant_id, unidade, bloco) WHERE tenant_id IS NOT NULL;

-- 3. Mantém unicidade global apenas para usuários SEM condomínio (Admin/Afiliado)
CREATE UNIQUE INDEX idx_users_email_global ON users (email) WHERE tenant_id IS NULL;

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    token VARCHAR(10) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- 4. CAMADA DE AFILIADOS
-- ====================================================================

CREATE TABLE afiliados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    codigo_ref VARCHAR(50) UNIQUE NOT NULL, 
    chave_pix VARCHAR(255) NOT NULL,
    
    saldo_disponivel NUMERIC(19, 2) DEFAULT 0.00,
    saldo_pendente NUMERIC(19, 2) DEFAULT 0.00,
    banco VARCHAR(100),
    tipo_chave VARCHAR(50),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Agora que a tabela afiliados existe, criamos a chave estrangeira em tenants
ALTER TABLE tenants ADD CONSTRAINT fk_tenants_afiliados FOREIGN KEY (afiliado_id) REFERENCES afiliados(id);

CREATE TABLE comissoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    afiliado_id UUID REFERENCES afiliados(id),
    condominio_pagante_id UUID REFERENCES tenants(id),
    valor NUMERIC(19, 2) NOT NULL,
    data_venda DATE NOT NULL,
    data_liberacao DATE NOT NULL, 
    status VARCHAR(20) DEFAULT 'BLOQUEADO', 
    asaas_transfer_id VARCHAR(50), 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- 5. ASSEMBLEIAS E VOTAÇÃO
-- ====================================================================

CREATE TABLE assemblies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    titulo VARCHAR(255) NOT NULL,
    description TEXT, 
    data_inicio TIMESTAMP NOT NULL,
    data_fim TIMESTAMP NOT NULL,
    link_video_conferencia VARCHAR(255),
    
    youtube_live_url VARCHAR(255), 
    relatorio_ia_url TEXT, 
    
    status VARCHAR(20) DEFAULT 'AGENDADA',
    anexo_url VARCHAR(255),
    tipo_assembleia VARCHAR(50) DEFAULT 'AGE',
    quorum_type VARCHAR(50) DEFAULT 'SIMPLE',
    vote_type VARCHAR(50) DEFAULT 'YES_NO_ABSTAIN',
    vote_privacy VARCHAR(50) DEFAULT 'OPEN',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE poll_options_assembly ( 
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    assembly_id UUID REFERENCES assemblies(id),
    descricao VARCHAR(255) NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assembly_id UUID NOT NULL REFERENCES assemblies(id),
    user_id UUID NOT NULL REFERENCES users(id),
    tenant_id UUID REFERENCES tenants(id),
    option_id VARCHAR(255) NOT NULL, 
    
    unidade VARCHAR(100), -- Unidade do morador para este voto
    
    hash VARCHAR(255),                                    
    fraction NUMERIC(10, 6) DEFAULT 0.0015,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Permite apenas 1 voto por Assembleia + Usuário + Unidade
    UNIQUE(assembly_id, user_id, unidade) 
);

CREATE TABLE chat_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assembly_id UUID NOT NULL REFERENCES assemblies(id),
    user_id UUID REFERENCES users(id),
    tenant_id UUID REFERENCES tenants(id),
    
    sender_name VARCHAR(255),         
    content TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'CHAT', 
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
);

-- ====================================================================
-- 6. GOVERNANÇA (ENQUETES RÁPIDAS E COMUNICADOS)
-- ====================================================================

CREATE TABLE polls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    title VARCHAR(255),
    description TEXT,
    status VARCHAR(20) DEFAULT 'OPEN',
    end_date TIMESTAMP,
    
    auto_archive_date TIMESTAMP,
    is_archived BOOLEAN DEFAULT FALSE,

    target_audience VARCHAR(50) DEFAULT 'ALL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id)
);

CREATE TABLE poll_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID REFERENCES polls(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE poll_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID REFERENCES polls(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    option_id UUID REFERENCES poll_options(id),
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    unit VARCHAR(100), 
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(poll_id, user_id, unit)
);

CREATE TABLE calendar_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50), 
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    title VARCHAR(255) NOT NULL,
    content TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    target_type VARCHAR(50),
    target_value VARCHAR(255), 
    requires_confirmation BOOLEAN DEFAULT FALSE,
    
    auto_archive_date TIMESTAMP,
    is_archived BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcement_reads (
    announcement_id UUID REFERENCES announcements(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
    PRIMARY KEY (announcement_id, user_id)
);

-- ====================================================================
-- 7. ÁREAS COMUNS E RESERVAS
-- ====================================================================

CREATE TABLE common_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50),
    capacity INTEGER,
    description TEXT,
    price NUMERIC(19, 2),
    requires_approval BOOLEAN DEFAULT FALSE,
    open_time VARCHAR(10),
    close_time VARCHAR(10),
    image_url VARCHAR(255),
    rules TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- 7.1 CONFIGURAÇÃO DE PAGAMENTO HÍBRIDO (ADICIONADO)
-- ====================================================================
CREATE TABLE IF NOT EXISTS tenant_payment_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    enable_asaas BOOLEAN DEFAULT FALSE,
    enable_manual_pix BOOLEAN DEFAULT TRUE,
    bank_name VARCHAR(255),
    agency VARCHAR(50),
    account VARCHAR(50),
    pix_key VARCHAR(255),
    instructions TEXT,
    asaas_access_token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    area_id UUID REFERENCES common_areas(id), 
    user_id UUID REFERENCES users(id),
    
    -- Dados Snapshot do Morador
    nome VARCHAR(255),
    cpf VARCHAR(14),
    unidade VARCHAR(50),
    bloco VARCHAR(50),
    unit VARCHAR(50), 
    whatsapp VARCHAR(20), -- Adicionado
    
    booking_date DATE,
    start_time VARCHAR(10),
    end_time VARCHAR(10),
    status VARCHAR(20), 
    
    billing_type VARCHAR(50), 
    
    total_price NUMERIC(19, 2),
    asaas_payment_id VARCHAR(50),
    
    receipt_url VARCHAR(255), -- Adicionado (se TEXT era melhor para URL longas, mas VARCHAR(255) serve)

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(255)
);

-- ====================================================================
-- 8. CHAMADOS, FINANCEIRO E AUDITORIA
-- ====================================================================

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'OPEN',
    priority VARCHAR(20) DEFAULT 'LOW',
    user_id UUID REFERENCES users(id),
    
    user_name VARCHAR(255),
    user_unit VARCHAR(50),
    user_block VARCHAR(50),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID REFERENCES tickets(id) ON DELETE CASCADE,
    sender_id UUID,
    sender_name VARCHAR(255),
    is_admin_sender BOOLEAN DEFAULT FALSE,
    message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE condo_financial (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    balance NUMERIC(19, 2),
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE financial_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    
    month VARCHAR(20) NOT NULL,
    year INTEGER NOT NULL,
    
    file_name VARCHAR(255),
    url TEXT NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    timestamp VARCHAR(50),        
    action VARCHAR(255),            
    
    user_id VARCHAR(255),        
    user_name VARCHAR(255),        
    
    tenant_id UUID REFERENCES tenants(id), 
    
    details TEXT,                 
    resource_type VARCHAR(255), 
    
    ip_address VARCHAR(45),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- 9. TABELA DE DISPOSITIVOS CONFIÁVEIS (2FA TRUST)
-- ====================================================================

CREATE TABLE trusted_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_identifier VARCHAR(255) NOT NULL, -- UUID gerado pelo front
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Garante que um device só aparece uma vez por usuário para evitar duplicidade
    UNIQUE(user_id, device_identifier)
);

-- ====================================================================
-- 10. ÍNDICES DE PERFORMANCE
-- ====================================================================
CREATE INDEX idx_chat_assembly ON chat_message(assembly_id);
CREATE INDEX idx_votes_assembly ON votes(assembly_id);
CREATE INDEX idx_reservations_date ON reservations(booking_date);
CREATE INDEX idx_comissoes_status ON comissoes(status);
CREATE INDEX idx_afiliados_codigo ON afiliados(codigo_ref);
CREATE INDEX idx_ticket_messages_ticket ON ticket_messages(ticket_id);
CREATE INDEX idx_tickets_tenant ON tickets(tenant_id);
CREATE INDEX idx_tickets_user ON tickets(user_id);
CREATE INDEX idx_planos_nome ON planos(nome);
CREATE INDEX idx_fin_reports_tenant ON financial_reports(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_tenant ON audit_logs(tenant_id);

CREATE INDEX idx_poll_votes_poll ON poll_votes(poll_id);
CREATE INDEX idx_calendar_events_tenant ON calendar_events(tenant_id);

CREATE INDEX idx_subscriptions_external_ref ON subscriptions(external_reference);
CREATE INDEX idx_subscriptions_tenant ON subscriptions(tenant_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

CREATE INDEX idx_trusted_lookup ON trusted_devices(user_id, device_identifier);

-- ====================================================================
-- 11. GESTÃO DE ENCOMENDAS (ORDERS)
-- ====================================================================

CREATE TABLE tb_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

    tracking_code VARCHAR(255) NOT NULL,
    origin VARCHAR(255),
    recipient_name VARCHAR(255) NOT NULL,
    arrival_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
    
    status VARCHAR(50) DEFAULT 'PENDING', 

    resident_id UUID NOT NULL REFERENCES users(id),
    resident_name VARCHAR(255),
    unit VARCHAR(50),        
    block VARCHAR(50),       
    resident_email VARCHAR(255),
    resident_cpf VARCHAR(20),
    resident_whatsapp VARCHAR(20),

    resident_signature_date TIMESTAMP,
    resident_signature_name VARCHAR(255),

    staff_signature_date TIMESTAMP,
    staff_signature_name VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_tenant ON tb_orders(tenant_id);
CREATE INDEX idx_orders_resident ON tb_orders(resident_id);
CREATE INDEX idx_orders_status ON tb_orders(status);
CREATE INDEX idx_orders_tracking ON tb_orders(tracking_code);
CREATE INDEX idx_orders_arrival ON tb_orders(arrival_date);

-- ====================================================================
-- 12. GESTÃO DE CONVIDADOS E CONTROLE DE ACESSO (GUESTS)
-- ====================================================================

CREATE TABLE tb_guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

    guest_name VARCHAR(255) NOT NULL,
    guest_rg VARCHAR(50) NOT NULL,
    scheduled_date TIMESTAMP, -- Novo: Data agendada
    access_code VARCHAR(255) UNIQUE NOT NULL, -- Token UUID para o QR Code
    status VARCHAR(50) DEFAULT 'PENDING',     -- PENDING, AUTHORIZED, CANCELED
    entry_time TIMESTAMP,                     -- Preenchido via PUT /authorize

    resident_id UUID NOT NULL REFERENCES users(id),
    resident_name VARCHAR(255),
    unit VARCHAR(50),        
    block VARCHAR(50),       
    resident_whatsapp VARCHAR(20),
    resident_cpf VARCHAR(20), -- Novo: CPF do Morador
    resident_email VARCHAR(255), -- Novo: Email do Morador

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_guests_tenant ON tb_guests(tenant_id);
CREATE INDEX idx_guests_access_code ON tb_guests(access_code);
CREATE INDEX idx_guests_resident ON tb_guests(resident_id);

-- ====================================================================
-- 12. DADOS INICIAIS (SEED DATA)
-- ====================================================================

-- 12.1 Planos
DELETE FROM planos;

INSERT INTO planos (id, nome, ciclo, max_unidades, preco_base, taxa_servico_reserva) VALUES
('00000000-0000-0000-0000-000000000000', 'Plano Cortesia Votzz', 'ANUAL', 999, 0.00, 0.00),
('11111111-1111-1111-1111-111111111111', 'Essencial Trimestral', 'TRIMESTRAL', 30, 570.00, 5.00),
('11111111-1111-1111-1111-222222222222', 'Essencial Anual',       'ANUAL',       30, 1980.00, 5.00),
('22222222-2222-2222-2222-111111111111', 'Business Trimestral',  'TRIMESTRAL', 80, 960.00, 0.00),
('22222222-2222-2222-2222-222222222222', 'Business Anual',        'ANUAL',       80, 3360.00, 0.00),
('33333333-3333-3333-3333-333333333333', 'Custom Trimestral',    'TRIMESTRAL', 9999, 0.00, 2.50),
('44444444-4444-4444-4444-444444444444', 'Custom Anual',           'ANUAL',       9999, 0.00, 2.50)
ON CONFLICT (id) DO UPDATE SET preco_base = EXCLUDED.preco_base;

-- 12.2 Cupom de Teste
INSERT INTO coupons (code, discount_percent, quantity, active, expiration_date)
VALUES ('VOTZZ10', 10.00, 100, true, '2030-12-31 23:59:59')
ON CONFLICT (code) DO NOTHING;

-- 12.3 Usuário Super Admin
INSERT INTO users (id, nome, email, password, role, cpf, whatsapp, tenant_id)
VALUES (
    '10000000-0000-0000-0000-000000000000',
    'Super Admin Votzz',
    'admin@votzz.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6abecRo.zCVJauOx.0YoTsFVu',
    'ADMIN',
    '000.000.000-00', 
    '11999999999', 
    NULL
) 
ON CONFLICT (email) WHERE tenant_id IS NULL 
DO UPDATE SET role = 'ADMIN';

-- 12.4 Afiliados
INSERT INTO users (id, nome, email, password, role, cpf, whatsapp, tenant_id) VALUES
('99999999-9999-9999-9999-999999999999', 'Carlos Afiliado', 'afiliado@votzz.com', '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6abecRo.zCVJauOx.0YoTsFVu', 'AFILIADO', '111.222.333-44', '71988887777', NULL)
ON CONFLICT (email) WHERE tenant_id IS NULL 
DO NOTHING;

INSERT INTO afiliados (id, user_id, codigo_ref, saldo_disponivel, saldo_pendente, chave_pix, banco, tipo_chave)
VALUES ('aff11111-1111-1111-1111-111111111111', '99999999-9999-9999-9999-999999999999', 'CARLOS10', 150.00, 500.00, 'afiliado@votzz.com', 'Nubank', 'EMAIL')
ON CONFLICT (id) DO NOTHING;

-- 12.5 Condomínio Solar das Águas
INSERT INTO tenants (
    id, nome, cnpj, plano_id, status_assinatura, unidades_total, blocos_total,
    cep, logradouro, numero, bairro, cidade, estado, 
    secret_keyword, ativo
) VALUES (
    '55555555-5555-5555-5555-555555555555', 
    'Condomínio Solar das Águas', 
    '12.345.678/0001-90', 
    '22222222-2222-2222-2222-222222222222', 
    'PAID', 50, 2,
    '40000-000', 'Av. Oceânica', '100', 'Barra', 'Salvador', 'BA',
    'SOLAR123', TRUE
) ON CONFLICT (cnpj) DO NOTHING;

-- 12.6 Síndico do Solar das Águas
INSERT INTO users (
    id, tenant_id, nome, email, password, role, cpf, whatsapp, unidade, bloco
) VALUES (
    '66666666-6666-6666-6666-666666666666',
    '55555555-5555-5555-5555-555555555555', 
    'João Síndico',
    'sindico@solar.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6abecRo.zCVJauOx.0YoTsFVu', 
    'SINDICO',
    '222.333.444-55',
    '71999998888',
    '101', 'A'
) 
ON CONFLICT (email, tenant_id, unidade, bloco) WHERE tenant_id IS NOT NULL 
DO NOTHING;

-- 12.7 SEED da Lista de Unidades do Síndico (Para evitar erro de @ElementCollection)
INSERT INTO user_unidades (user_id, unidade_info)
VALUES ('66666666-6666-6666-6666-666666666666', '101 - A');

-- 13. COMISSÕES E FINANCEIRO INICIAL
INSERT INTO comissoes (
    id, afiliado_id, condominio_pagante_id, valor, 
    data_venda, data_liberacao, status, created_at
) VALUES 
(
    gen_random_uuid(),
    'aff11111-1111-1111-1111-111111111111', 
    '55555555-5555-5555-5555-555555555555', 
    150.00,
    CURRENT_DATE - INTERVAL '45 days',
    CURRENT_DATE - INTERVAL '15 days',
    'DISPONIVEL',
    CURRENT_TIMESTAMP - INTERVAL '45 days'
),
(
    gen_random_uuid(),
    'aff11111-1111-1111-1111-111111111111', 
    '55555555-5555-5555-5555-555555555555', 
    500.00,
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '30 days',
    'BLOQUEADO',
    CURRENT_TIMESTAMP
);

-- Inicializa o caixa do condomínio (Evita erro visual no dashboard)
INSERT INTO condo_financial (tenant_id, balance, updated_by)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    15000.00,
    'Sistema Inicial'
);

-- ====================================================================
-- 14. CORREÇÃO CRÍTICA: VÍNCULO MANY-TO-MANY (SÍNDICO <-> TENANT)
-- ====================================================================

INSERT INTO user_tenants (user_id, tenant_id)
VALUES (
    '66666666-6666-6666-6666-666666666666', -- ID do João Síndico
    '55555555-5555-5555-5555-555555555555'  -- ID do Condomínio Solar
)
ON CONFLICT (user_id, tenant_id) DO NOTHING;

-- ====================================================================
-- 15. CONVIDADOS (GUESTS) INICIAIS
-- ====================================================================

INSERT INTO tb_guests (
    id, tenant_id, guest_name, guest_rg, access_code, status, resident_id, resident_name, unit, block, resident_whatsapp, scheduled_date, resident_cpf, resident_email
) VALUES (
    gen_random_uuid(),
    '55555555-5555-5555-5555-555555555555', -- Tenant Teste
    'Carlos Visitante',
    '11.222.333-4',
    gen_random_uuid(),
    'PENDING',
    '66666666-6666-6666-6666-666666666666', -- User Morador Teste (João Síndico)
    'João Síndico',
    '101',
    'A',
    '71999998888',
    CURRENT_TIMESTAMP + INTERVAL '1 day',
    '222.333.444-55',
    'sindico@solar.com'
);