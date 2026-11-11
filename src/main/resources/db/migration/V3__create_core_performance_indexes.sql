-- ============================================================================
-- Migration V3: Indices Compostos de Performance para Consultas B2B e Jobs
-- ============================================================================

CREATE INDEX idx_accreditation_plans_ativo
    ON accreditation_plans(ativo);

CREATE INDEX idx_partner_orgs_vertical_status
    ON partner_organizations(vertical_tech, status);

CREATE INDEX idx_partner_orgs_plano
    ON partner_organizations(plano_id);

CREATE INDEX idx_network_benefits_status_validade
    ON network_benefits(status, valido_ate);

CREATE INDEX idx_network_benefits_ofertante
    ON network_benefits(organizacao_ofertante_id);

CREATE INDEX idx_csv_batches_status
    ON csv_import_batches(status);
