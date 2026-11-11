-- ============================================================================
-- Migration V1: Tabelas de Planos de Credenciamento (Tiers) e Organizacoes Parceiras
-- ============================================================================

CREATE TABLE accreditation_plans (
    id UUID PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    valor_mensalidade NUMERIC(12, 2) NOT NULL,
    max_beneficios_ofertados INTEGER NOT NULL,
    cota_resgates_mensal INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE partner_organizations (
    id UUID PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    nome_fantasia VARCHAR(150) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    email_contato VARCHAR(150) NOT NULL,
    vertical_tech VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    plano_id UUID NOT NULL,
    lote_origem_id UUID,
    saldo_resgates_ciclo INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_partner_org_plan FOREIGN KEY (plano_id) REFERENCES accreditation_plans(id)
);
