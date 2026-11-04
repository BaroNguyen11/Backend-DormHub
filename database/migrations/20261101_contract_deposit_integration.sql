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

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.contracts
               WHERE student_id IS NULL OR room_id IS NULL OR start_date IS NULL OR end_date IS NULL
                  OR monthly_rent IS NULL OR status IS NULL OR deposit_status IS NULL
                  OR created_at IS NULL OR updated_at IS NULL) THEN
        RAISE EXCEPTION 'Cannot align contracts with the Contract entity: required fields contain NULL values';
    END IF;
END $$;

ALTER TABLE public.contracts
    ALTER COLUMN student_id SET NOT NULL,
    ALTER COLUMN room_id SET NOT NULL,
    ALTER COLUMN start_date SET NOT NULL,
    ALTER COLUMN end_date SET NOT NULL,
    ALTER COLUMN monthly_rent TYPE NUMERIC(38, 0) USING monthly_rent::numeric(38, 0),
    ALTER COLUMN monthly_rent SET NOT NULL,
    ALTER COLUMN deposit_amount TYPE NUMERIC(38, 0) USING deposit_amount::numeric(38, 0),
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN deposit_status SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

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
    ADD COLUMN IF NOT EXISTS deposit_marker SMALLINT,
    ADD COLUMN IF NOT EXISTS total_amount NUMERIC(38, 0);

UPDATE public.invoices
SET invoice_type = CASE
    WHEN deposit_marker::text IN ('1', 'true', 't') THEN 'DEPOSIT'
    ELSE 'UTILITY'
END
WHERE invoice_type IS NULL;

ALTER TABLE public.invoices
    ALTER COLUMN invoice_type TYPE VARCHAR(32) USING invoice_type::text,
    ALTER COLUMN invoice_type SET DEFAULT 'UTILITY',
    ALTER COLUMN invoice_type SET NOT NULL,
    ALTER COLUMN deposit_marker TYPE SMALLINT USING CASE
        WHEN deposit_marker IS NULL THEN NULL
        WHEN deposit_marker::text IN ('1', 'true', 't') THEN 1
        ELSE 0
    END,
    ALTER COLUMN status TYPE VARCHAR(32)
        USING CASE status::text
            WHEN 'PENDING' THEN 'UNPAID'
            WHEN 'OVERDUE' THEN 'UNPAID'
            ELSE status::text
        END,
    ALTER COLUMN issue_date TYPE TIMESTAMP WITHOUT TIME ZONE USING issue_date::timestamp,
    ALTER COLUMN due_date TYPE TIMESTAMP WITHOUT TIME ZONE USING due_date::timestamp,
    ALTER COLUMN billing_month TYPE DATE USING billing_month::date,
    -- DEPOSIT invoices are tied to a contract and do not represent a billing month.
    ALTER COLUMN billing_month DROP NOT NULL,
    ALTER COLUMN room_id DROP NOT NULL;

-- Keep notification persistence compatible with the Notification entity in main.
DO $$
BEGIN
    IF to_regclass('public.notifications') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'content')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'body') THEN
        ALTER TABLE public.notifications RENAME COLUMN content TO body;
    END IF;
END $$;

ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS body TEXT,
    ADD COLUMN IF NOT EXISTS ref_id BIGINT;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'notifications'
                 AND column_name = 'maintenance_request_id') THEN
        ALTER TABLE public.notifications ALTER COLUMN maintenance_request_id DROP NOT NULL;
    END IF;
END $$;

ALTER TABLE public.invoice_details
    ADD COLUMN IF NOT EXISTS amount NUMERIC(38, 0);

UPDATE public.invoice_details
SET amount = quantity * unit_price
WHERE amount IS NULL AND quantity IS NOT NULL AND unit_price IS NOT NULL;

ALTER TABLE public.invoice_details
    ALTER COLUMN item_type TYPE VARCHAR(32) USING item_type::text;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.invoices invoice
               WHERE invoice.total_amount IS NULL
                 AND NOT EXISTS (SELECT 1 FROM public.invoice_details detail
                                 WHERE detail.invoice_id = invoice.invoice_id
                                   AND detail.amount IS NOT NULL)) THEN
        RAISE EXCEPTION 'Cannot infer total_amount for an invoice with no valid detail rows';
    END IF;
    IF EXISTS (SELECT 1 FROM public.invoice_details
               WHERE invoice_id IS NULL OR item_type IS NULL OR description IS NULL
                  OR quantity IS NULL OR unit_price IS NULL OR amount IS NULL) THEN
        RAISE EXCEPTION 'Cannot align invoice_details with the InvoiceDetail entity: required fields contain NULL values';
    END IF;
    IF EXISTS (SELECT 1 FROM public.invoices WHERE issue_date IS NULL OR status IS NULL) THEN
        RAISE EXCEPTION 'Cannot align invoices with the Invoice entity: issue_date or status contains NULL values';
    END IF;
END $$;

UPDATE public.invoices invoice
SET total_amount = (
    SELECT SUM(detail.amount)
    FROM public.invoice_details detail
    WHERE detail.invoice_id = invoice.invoice_id
)
WHERE invoice.total_amount IS NULL;

ALTER TABLE public.invoices
    ALTER COLUMN total_amount TYPE NUMERIC(38, 0) USING total_amount::numeric(38, 0),
    ALTER COLUMN total_amount SET NOT NULL,
    ALTER COLUMN issue_date SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE public.invoice_details
    ALTER COLUMN item_type SET NOT NULL,
    ALTER COLUMN description SET NOT NULL,
    ALTER COLUMN quantity SET NOT NULL,
    ALTER COLUMN unit_price SET NOT NULL,
    ALTER COLUMN amount TYPE NUMERIC(38, 0) USING amount::numeric(38, 0),
    ALTER COLUMN amount SET NOT NULL;

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
    IF EXISTS (SELECT 1 FROM public.room_assignments
               WHERE student_id IS NULL OR contract_id IS NULL OR room_id IS NULL
                  OR assigned_by IS NULL OR assigned_at IS NULL OR status IS NULL) THEN
        RAISE EXCEPTION 'Cannot align room_assignments with the RoomAssignment entity: required fields contain NULL values';
    END IF;
END $$;

ALTER TABLE public.room_assignments
    ALTER COLUMN student_id SET NOT NULL,
    ALTER COLUMN contract_id SET NOT NULL,
    ALTER COLUMN room_id SET NOT NULL,
    ALTER COLUMN assigned_by SET NOT NULL,
    ALTER COLUMN assigned_at SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

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
