-- ============================================================================
-- Migration V2: Tabelas de Lotes de Importacao CSV e Beneficios B2B da Rede
-- ============================================================================

CREATE TABLE csv_import_batches (
    id UUID PRIMARY KEY,
    nome_arquivo VARCHAR(255) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    total_linhas INTEGER NOT NULL DEFAULT 0,
    linhas_sucesso INTEGER NOT NULL DEFAULT 0,
    linhas_erro INTEGER NOT NULL DEFAULT 0,
    resumo_erros TEXT,
    iniciado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalizado_em TIMESTAMP
);

ALTER TABLE partner_organizations
    ADD CONSTRAINT fk_partner_org_csv_batch
    FOREIGN KEY (lote_origem_id) REFERENCES csv_import_batches(id);

CREATE TABLE network_benefits (
    id UUID PRIMARY KEY,
    organizacao_ofertante_id UUID NOT NULL,
    plano_minimo_id UUID NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    cota_total INTEGER NOT NULL,
    cota_utilizada INTEGER NOT NULL DEFAULT 0,
    valido_ate DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_network_benefit_org FOREIGN KEY (organizacao_ofertante_id) REFERENCES partner_organizations(id),
    CONSTRAINT fk_network_benefit_min_plan FOREIGN KEY (plano_minimo_id) REFERENCES accreditation_plans(id)
);
