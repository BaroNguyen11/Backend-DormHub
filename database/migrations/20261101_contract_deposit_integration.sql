BEGIN;

-- Keep the legacy room-registration link nullable while new contracts link
-- directly to the approved DormApplication.
ALTER TABLE public.contracts
    ALTER COLUMN registration_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS dorm_application_id BIGINT,
    ADD COLUMN IF NOT EXISTS room_id BIGINT;

ALTER TABLE public.rooms
    ADD COLUMN IF NOT EXISTS reserved_occupancy INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_rooms_capacity_occupancy'
          AND conrelid = 'public.rooms'::regclass
    ) THEN
        ALTER TABLE public.rooms
            ADD CONSTRAINT chk_rooms_capacity_occupancy
            CHECK (capacity > 0 AND current_occupancy >= 0 AND reserved_occupancy >= 0
                AND current_occupancy + reserved_occupancy <= capacity);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_contract_dorm_application') THEN
        ALTER TABLE public.contracts
            ADD CONSTRAINT fk_contract_dorm_application
            FOREIGN KEY (dorm_application_id) REFERENCES public.dorm_applications(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_contract_room') THEN
        ALTER TABLE public.contracts
            ADD CONSTRAINT fk_contract_room
            FOREIGN KEY (room_id) REFERENCES public.rooms(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_contract_dorm_application
    ON public.contracts (dorm_application_id)
    WHERE dorm_application_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_contracts_room_status
    ON public.contracts (room_id, status);

-- A student may have historical contracts, but only one pending or active one.
ALTER TABLE public.contracts DROP CONSTRAINT IF EXISTS uq_contract_student;
CREATE UNIQUE INDEX IF NOT EXISTS uq_contract_student_current
    ON public.contracts (student_id)
    WHERE status IN ('PENDING', 'ACTIVE');

-- Deposit invoices share the existing invoices table so Payment can use its
-- existing invoice_id foreign key. Existing invoices are treated as utility.
ALTER TABLE public.invoices
    ADD COLUMN IF NOT EXISTS contract_id BIGINT,
    ADD COLUMN IF NOT EXISTS invoice_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS total_amount NUMERIC(38, 0);

UPDATE public.invoices
SET invoice_type = 'UTILITY'
WHERE invoice_type IS NULL;

ALTER TABLE public.invoices
    ALTER COLUMN invoice_type SET DEFAULT 'UTILITY',
    ALTER COLUMN invoice_type SET NOT NULL;

ALTER TABLE public.invoice_details
    ADD COLUMN IF NOT EXISTS amount NUMERIC(38, 0);

UPDATE public.invoice_details
SET amount = quantity * unit_price
WHERE amount IS NULL AND quantity IS NOT NULL AND unit_price IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoice_contract') THEN
        ALTER TABLE public.invoices
            ADD CONSTRAINT fk_invoice_contract
            FOREIGN KEY (contract_id) REFERENCES public.contracts(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_contract_deposit
    ON public.invoices (contract_id)
    WHERE contract_id IS NOT NULL AND invoice_type = 'DEPOSIT';
CREATE INDEX IF NOT EXISTS idx_invoices_contract
    ON public.invoices (contract_id);

ALTER TABLE public.room_assignments
    ADD COLUMN IF NOT EXISTS contract_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_room_assignments_contract') THEN
        ALTER TABLE public.room_assignments
            ADD CONSTRAINT fk_room_assignments_contract
            FOREIGN KEY (contract_id) REFERENCES public.contracts(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_room_assignments_contract
    ON public.room_assignments (contract_id)
    WHERE contract_id IS NOT NULL;

COMMIT;
