--liquibase formatted sql

--changeset simplehearing:089-migrate-legacy-assessments splitStatements:false
-- Backfills existing ISAA/PRBA rows (item_scores JSON blob) onto the new generic
-- assessment_items/assessment_item_options schema, then drops the legacy columns.
DO $$
DECLARE
    pa RECORD;
    def_id UUID;
    kv RECORD;
    item_row RECORD;
    option_row RECORD;
BEGIN
    FOR pa IN SELECT id, assessment_type, item_scores FROM patient_assessments WHERE assessment_type IS NOT NULL LOOP
        SELECT id INTO def_id FROM assessment_definitions WHERE code = pa.assessment_type;
        IF def_id IS NULL THEN
            CONTINUE;
        END IF;

        UPDATE patient_assessments SET definition_id = def_id WHERE id = pa.id;

        FOR kv IN SELECT key, value::int AS score FROM jsonb_each_text(pa.item_scores::jsonb) LOOP
            SELECT ai.id INTO item_row FROM assessment_items ai
                JOIN assessment_categories ac ON ac.id = ai.category_id
                WHERE ac.definition_id = def_id AND ai.item_number = kv.key::int
                LIMIT 1;
            IF item_row.id IS NULL THEN
                CONTINUE;
            END IF;

            SELECT id INTO option_row FROM assessment_item_options
                WHERE item_id = item_row.id AND score = kv.score LIMIT 1;

            INSERT INTO patient_assessment_responses (id, patient_assessment_id, item_id, selected_option_id)
            VALUES (gen_random_uuid(), pa.id, item_row.id, option_row.id);
        END LOOP;
    END LOOP;
END $$;

ALTER TABLE patient_assessments ALTER COLUMN definition_id SET NOT NULL;
ALTER TABLE patient_assessments DROP COLUMN assessment_type;
ALTER TABLE patient_assessments DROP COLUMN item_scores;

--rollback ALTER TABLE patient_assessments ADD COLUMN assessment_type VARCHAR(20);
--rollback ALTER TABLE patient_assessments ADD COLUMN item_scores TEXT;
--rollback ALTER TABLE patient_assessments ALTER COLUMN definition_id DROP NOT NULL;
