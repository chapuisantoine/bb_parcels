-- Parcelles liées à une région WorldGuard.
CREATE TABLE IF NOT EXISTS bb_parcels (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    server_id   VARCHAR(32)  NOT NULL,
    region_id   VARCHAR(128) NOT NULL,
    world       VARCHAR(64)  NOT NULL,
    label       VARCHAR(64)  NOT NULL,
    description TEXT         DEFAULT NULL,
    status      ENUM('available','rented','disabled') NOT NULL DEFAULT 'available',
    created_at  DATETIME     NOT NULL DEFAULT NOW(),
    updated_at  DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    UNIQUE KEY uq_parcel_label (server_id, label),
    UNIQUE KEY uq_parcel_region (server_id, region_id, world)
);

-- Locations actives ou terminées.
CREATE TABLE IF NOT EXISTS bb_rentals (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parcel_id   INT          NOT NULL,
    tenant_uuid VARCHAR(36)  NOT NULL,
    start_date  DATETIME     NOT NULL DEFAULT NOW(),
    end_date    DATETIME     DEFAULT NULL,
    status      ENUM('active','expired','terminated') NOT NULL DEFAULT 'active',
    created_at  DATETIME     NOT NULL DEFAULT NOW(),
    updated_at  DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    CONSTRAINT fk_rental_parcel FOREIGN KEY (parcel_id) REFERENCES bb_parcels(id)
);

-- Membres autorisés par le locataire sur sa location active.
CREATE TABLE IF NOT EXISTS bb_rental_members (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rental_id   INT          NOT NULL,
    member_uuid VARCHAR(36)  NOT NULL,
    role        ENUM('owner', 'member') NOT NULL DEFAULT 'member',
    added_at    DATETIME     NOT NULL DEFAULT NOW(),
    UNIQUE KEY uq_rental_member (rental_id, member_uuid),
    CONSTRAINT fk_member_rental FOREIGN KEY (rental_id) REFERENCES bb_rentals(id)
);