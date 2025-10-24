-- =============================================================================
-- Script de création des tables pour l'application PayHint
-- Base de données : PostgreSQL
-- Version : UUID pour les clés primaires
-- =============================================================================

-- Activer l'extension pgcrypto si elle n'est pas déjà disponible (pour gen_random_uuid())
-- Dans les versions récentes de PostgreSQL, cette fonction est souvent intégrée.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Suppression des anciens types et tables si elles existent, pour un script ré-exécutable
DROP TABLE IF EXISTS notification_logs, payments, installments, invoices, templates, clients, user_settings, users CASCADE;
DROP TYPE IF EXISTS installment_status_enum, notification_status_enum;

-- =============================================================================
-- Création des types ENUM
-- =============================================================================

CREATE TYPE installment_status_enum AS ENUM (
    'PENDING',
    'LATE',
    'PARTIALLY_PAID',
    'PAID',
    'CANCELLED'
);

CREATE TYPE notification_status_enum AS ENUM (
    'SENT',
    'FAILED'
);


-- =============================================================================
-- Création des tables
-- =============================================================================

-- Table des utilisateurs (freelances)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table des préférences utilisateur (relation un-à-un avec users)
CREATE TABLE user_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    currency VARCHAR(10) NOT NULL DEFAULT 'EUR',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Paris',
    language VARCHAR(10) NOT NULL DEFAULT 'fr-FR',
    reminder_schedule JSONB
);

-- Table des clients liés à un utilisateur
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_company_name UNIQUE (user_id, company_name)
);

-- Table des modèles de messages personnalisés par l'utilisateur
CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    CONSTRAINT uq_user_template_name UNIQUE (user_id, name)
);

-- Table des factures (le "dossier" global)
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    invoice_reference VARCHAR(255) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    currency VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_client_invoice_reference UNIQUE (client_id, invoice_reference)
);

-- Table des échéances liées à une facture
CREATE TABLE installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount_due NUMERIC(12, 2) NOT NULL CHECK (amount_due >= 0),
    amount_paid NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (amount_paid >= 0),
    due_date DATE NOT NULL,
    status installment_status_enum NOT NULL DEFAULT 'PENDING'
);

-- Table des paiements/versements reçus pour une échéance
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installment_id UUID NOT NULL REFERENCES installments(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    payment_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table pour l'historique des notifications de relance envoyées
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installment_id UUID NOT NULL REFERENCES installments(id) ON DELETE CASCADE,
    recipient_address VARCHAR(255) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status notification_status_enum NOT NULL
);


-- =============================================================================
-- Création des INDEX pour optimiser les performances
-- =============================================================================

CREATE INDEX idx_invoices_on_client_id ON invoices(client_id);
CREATE INDEX idx_installments_on_invoice_id ON installments(invoice_id);
CREATE INDEX idx_installments_on_status_and_due_date ON installments(status, due_date);
CREATE INDEX idx_payments_on_installment_id ON payments(installment_id);

-- =============================================================================
-- Fin du script
-- =============================================================================