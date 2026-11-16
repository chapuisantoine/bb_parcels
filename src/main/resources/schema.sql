-- Parcelles liées à une région WorldGuard.
CREATE TABLE IF NOT EXISTS bb_parcels (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    server_id   VARCHAR(32)  NOT NULL,
    region_id   VARCHAR(128) NOT NULL,
    world       VARCHAR(64)  NOT NULL,
    label       VARCHAR(64)  NOT NULL,
    description TEXT         DEFAULT NULL,
    weekly_price DECIMAL(10,2) NOT NULL,
    status      ENUM('available','rented','disabled') NOT NULL DEFAULT 'available',
    created_at  DATETIME     NOT NULL DEFAULT NOW(),
    updated_at  DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    UNIQUE KEY uq_parcel_label (server_id, label),
    UNIQUE KEY uq_parcel_region (server_id, region_id, world),
    CONSTRAINT chk_parcel_price CHECK (weekly_price > 0)
);

-- Locations actives ou terminées.
CREATE TABLE IF NOT EXISTS bb_rentals (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parcel_id   INT          NOT NULL,
    tenant_profile_id INT  NOT NULL,
    start_date  DATETIME     NOT NULL DEFAULT NOW(),
    next_due_date DATETIME   NOT NULL,
    end_date    DATETIME     DEFAULT NULL,
    status      ENUM('active','expired','terminated') NOT NULL DEFAULT 'active',
    created_at  DATETIME     NOT NULL DEFAULT NOW(),
    updated_at  DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    INDEX idx_rental_due (status, next_due_date),
    CONSTRAINT fk_rental_parcel FOREIGN KEY (parcel_id) REFERENCES bb_parcels(id),
    CONSTRAINT fk_rental_profile FOREIGN KEY (tenant_profile_id) REFERENCES profiles(id)

);

-- Membres autorisés par le locataire sur sa location active.
CREATE TABLE IF NOT EXISTS bb_rental_members (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rental_id   INT          NOT NULL,
    member_profile_id INT  NOT NULL,
    role        ENUM('owner', 'member') NOT NULL DEFAULT 'member',
    added_at    DATETIME     NOT NULL DEFAULT NOW(),
    UNIQUE KEY uq_rental_member (rental_id, member_profile_id),
    CONSTRAINT fk_member_rental FOREIGN KEY (rental_id) REFERENCES bb_rentals(id),
    CONSTRAINT fk_member_profile FOREIGN KEY (member_profile_id) REFERENCES profiles(id)
);

-- Un loyer payé par semaine et par location : la clé unique empêche de payer deux fois la même période.
CREATE TABLE IF NOT EXISTS bb_rent_payments (
    id                  INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rental_id           INT           NOT NULL,
    period_index        INT           NOT NULL,
    due_at              DATETIME      NOT NULL,
    amount              DECIMAL(10,2) NOT NULL,
    paid_at             DATETIME      NOT NULL DEFAULT NOW(),
    bank_transaction_id INT           DEFAULT NULL,
    UNIQUE KEY uq_rent_period (rental_id, period_index),
    CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES bb_rentals(id)
);