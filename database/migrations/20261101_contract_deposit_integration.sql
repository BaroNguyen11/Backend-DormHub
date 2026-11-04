-- Align the housing workflow with Contract, Invoice, InvoiceDetail, and
-- RoomAssignment entities from main, then add DormApplication relations.
-- Apply after 20261101_room_assignment_schema.sql.
BEGIN;

-- Older local schema revisions used id/month instead of the main column names.
DO $$
BEGIN
    IF to_regclass('public.contracts') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'contracts' AND column_name = 'id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'contracts' AND column_name = 'contract_id') THEN
        ALTER TABLE public.contracts RENAME COLUMN id TO contract_id;
    END IF;
    IF to_regclass('public.invoices') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'invoices' AND column_name = 'id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'invoices' AND column_name = 'invoice_id') THEN
        ALTER TABLE public.invoices RENAME COLUMN id TO invoice_id;
    END IF;
    IF to_regclass('public.invoices') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'invoices' AND column_name = 'month')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'invoices' AND column_name = 'billing_month') THEN
        ALTER TABLE public.invoices RENAME COLUMN month TO billing_month;
    END IF;
    IF to_regclass('public.invoice_details') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'invoice_details' AND column_name = 'id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'invoice_details' AND column_name = 'detail_id') THEN
        ALTER TABLE public.invoice_details RENAME COLUMN id TO detail_id;
    END IF;
    IF to_regclass('public.room_assignments') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'room_assignments' AND column_name = 'id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'room_assignments' AND column_name = 'assignment_id') THEN
        ALTER TABLE public.room_assignments RENAME COLUMN id TO assignment_id;
    END IF;
END $$;

ALTER TABLE public.contracts
    ADD COLUMN IF NOT EXISTS dorm_application_id BIGINT,
    ADD COLUMN IF NOT EXISTS room_id BIGINT,
    ADD COLUMN IF NOT EXISTS deposit_status VARCHAR(16) NOT NULL DEFAULT 'UNPAID';

-- This partial index may have been created while status was a PostgreSQL enum.
-- Recreate it after converting status to the VARCHAR enum mapping used by main.
DROP INDEX IF EXISTS public.uq_contract_student_current;

-- A legacy room-registration reference is not used by the main Contract entity.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'contracts' AND column_name = 'registration_id') THEN
        ALTER TABLE public.contracts ALTER COLUMN registration_id DROP NOT NULL;
    END IF;
END $$;

ALTER TABLE public.contracts
    ALTER COLUMN status TYPE VARCHAR(32) USING status::text,
    ALTER COLUMN deposit_status TYPE VARCHAR(16) USING deposit_status::text,
    ALTER COLUMN start_date TYPE TIMESTAMP WITHOUT TIME ZONE USING start_date::timestamp,
    ALTER COLUMN end_date TYPE TIMESTAMP WITHOUT TIME ZONE USING end_date::timestamp;

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

ALTER TABLE public.dorm_applications
    ADD COLUMN IF NOT EXISTS processed_by BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_dorm_applications_processed_by'
                     AND conrelid = 'public.dorm_applications'::regclass) THEN
        ALTER TABLE public.dorm_applications
            ADD CONSTRAINT fk_dorm_applications_processed_by
            FOREIGN KEY (processed_by) REFERENCES public.users (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_contract_dorm_application'
                     AND conrelid = 'public.contracts'::regclass) THEN
        ALTER TABLE public.contracts
            ADD CONSTRAINT fk_contract_dorm_application
            FOREIGN KEY (dorm_application_id) REFERENCES public.dorm_applications (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_contract_dorm_application
    ON public.contracts (dorm_application_id)
    WHERE dorm_application_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_contracts_room_status
    ON public.contracts (room_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_contract_student_current
    ON public.contracts (student_id)
    WHERE status IN ('PENDING', 'ACTIVE');

ALTER TABLE public.invoices
    ADD COLUMN IF NOT EXISTS contract_id BIGINT,
    ADD COLUMN IF NOT EXISTS invoice_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS total_amount NUMERIC(38, 0);

UPDATE public.invoices
SET invoice_type = 'UTILITY'
WHERE invoice_type IS NULL;

ALTER TABLE public.invoices
    ALTER COLUMN invoice_type TYPE VARCHAR(32) USING invoice_type::text,
    ALTER COLUMN invoice_type SET DEFAULT 'UTILITY',
    ALTER COLUMN invoice_type SET NOT NULL,
    ALTER COLUMN status TYPE VARCHAR(32)
        USING CASE status::text
            WHEN 'PENDING' THEN 'UNPAID'
            WHEN 'OVERDUE' THEN 'UNPAID'
            ELSE status::text
        END,
    ALTER COLUMN issue_date TYPE TIMESTAMP WITHOUT TIME ZONE USING issue_date::timestamp,
    ALTER COLUMN due_date TYPE TIMESTAMP WITHOUT TIME ZONE USING due_date::timestamp;

ALTER TABLE public.invoice_details
    ADD COLUMN IF NOT EXISTS amount NUMERIC(38, 0);

UPDATE public.invoice_details
SET amount = quantity * unit_price
WHERE amount IS NULL AND quantity IS NOT NULL AND unit_price IS NOT NULL;

ALTER TABLE public.invoice_details
    ALTER COLUMN item_type TYPE VARCHAR(32) USING item_type::text;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_invoice_contract'
                     AND conrelid = 'public.invoices'::regclass) THEN
        ALTER TABLE public.invoices
            ADD CONSTRAINT fk_invoice_contract
            FOREIGN KEY (contract_id) REFERENCES public.contracts (contract_id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_contract_deposit
    ON public.invoices (contract_id)
    WHERE contract_id IS NOT NULL AND invoice_type = 'DEPOSIT';
CREATE INDEX IF NOT EXISTS idx_invoices_contract
    ON public.invoices (contract_id);

-- Keep the main RoomAssignment shape and backfill values from the application
-- relation when an earlier migration created the legacy shape.
ALTER TABLE public.room_assignments
    ADD COLUMN IF NOT EXISTS student_id BIGINT,
    ADD COLUMN IF NOT EXISTS contract_id BIGINT,
    ADD COLUMN IF NOT EXISTS room_id BIGINT,
    ADD COLUMN IF NOT EXISTS assigned_by BIGINT,
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS check_in_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS check_out_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ASSIGNED',
    ADD COLUMN IF NOT EXISTS dorm_application_id BIGINT;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'room_assignments'
                 AND column_name = 'dorm_application_id') THEN
        ALTER TABLE public.room_assignments ALTER COLUMN dorm_application_id DROP NOT NULL;
    END IF;
END $$;

UPDATE public.room_assignments assignment
SET student_id = application.student_id
FROM public.dorm_applications application
WHERE assignment.dorm_application_id = application.id
  AND assignment.student_id IS NULL;

UPDATE public.room_assignments assignment
SET contract_id = contract.contract_id
FROM public.contracts contract
JOIN public.dorm_applications application
  ON contract.dorm_application_id = application.id
WHERE assignment.dorm_application_id = application.id
  AND assignment.contract_id IS NULL;

UPDATE public.room_assignments assignment
SET assigned_by = application.processed_by
FROM public.dorm_applications application
WHERE assignment.dorm_application_id = application.id
  AND assignment.assigned_by IS NULL
  AND application.processed_by IS NOT NULL;

ALTER TABLE public.room_assignments
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_room_assignments_student'
                     AND conrelid = 'public.room_assignments'::regclass) THEN
        ALTER TABLE public.room_assignments
            ADD CONSTRAINT fk_room_assignments_student
            FOREIGN KEY (student_id) REFERENCES public.students (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_room_assignments_contract'
                     AND conrelid = 'public.room_assignments'::regclass) THEN
        ALTER TABLE public.room_assignments
            ADD CONSTRAINT fk_room_assignments_contract
            FOREIGN KEY (contract_id) REFERENCES public.contracts (contract_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_room_assignments_room'
                     AND conrelid = 'public.room_assignments'::regclass) THEN
        ALTER TABLE public.room_assignments
            ADD CONSTRAINT fk_room_assignments_room
            FOREIGN KEY (room_id) REFERENCES public.rooms (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_room_assignments_assigned_by'
                     AND conrelid = 'public.room_assignments'::regclass) THEN
        ALTER TABLE public.room_assignments
            ADD CONSTRAINT fk_room_assignments_assigned_by
            FOREIGN KEY (assigned_by) REFERENCES public.users (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_room_assignments_contract
    ON public.room_assignments (contract_id)
    WHERE contract_id IS NOT NULL;

COMMIT;
