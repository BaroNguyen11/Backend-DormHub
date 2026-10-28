ALTER TABLE contracts ADD COLUMN deposit_amount NUMERIC(38, 0);
ALTER TABLE contracts ADD CONSTRAINT ck_contracts_deposit_amount
    CHECK (deposit_amount IS NULL OR deposit_amount >= 0);

ALTER TABLE invoices ALTER COLUMN billing_month DROP NOT NULL;
ALTER TABLE invoices ALTER COLUMN due_date DROP NOT NULL;
ALTER TABLE invoices ADD COLUMN deposit_marker SMALLINT;
ALTER TABLE invoices DROP CONSTRAINT ck_invoices_owner;
ALTER TABLE invoices ADD CONSTRAINT ck_invoices_owner CHECK (
    (invoice_type = 'UTILITY' AND room_id IS NOT NULL AND contract_id IS NULL
        AND billing_month IS NOT NULL AND due_date IS NOT NULL AND deposit_marker IS NULL)
    OR (invoice_type = 'ROOM_FEE' AND room_id IS NULL AND contract_id IS NOT NULL
        AND billing_month IS NOT NULL AND due_date IS NOT NULL AND deposit_marker IS NULL)
    OR (invoice_type = 'DEPOSIT' AND room_id IS NULL AND contract_id IS NOT NULL
        AND billing_month IS NULL AND due_date IS NULL AND deposit_marker = 1)
);
ALTER TABLE invoices ADD CONSTRAINT uk_invoices_contract_deposit_marker
    UNIQUE (contract_id, deposit_marker);

ALTER TABLE invoice_details DROP CONSTRAINT ck_invoice_details_item_type;
ALTER TABLE invoice_details ADD CONSTRAINT ck_invoice_details_item_type
    CHECK (item_type IN ('ROOM', 'ELECTRICITY', 'WATER', 'DEPOSIT'));
