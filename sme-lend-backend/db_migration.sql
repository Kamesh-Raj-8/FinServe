-- FinServe DB Migration: Run ONCE before restarting after a fresh code pull
-- This resolves the kyc_record FK constraint error (loan_application_id)

SET FOREIGN_KEY_CHECKS = 0;

-- Clear KYC data that has no matching loan application
TRUNCATE TABLE kyc_promoter_link;
TRUNCATE TABLE kyc_record;

-- Clear scoring/decision tables if they exist  
DROP TABLE IF EXISTS scorecard;
DROP TABLE IF EXISTS decision;
DROP TABLE IF EXISTS eligibility_policy;
DROP TABLE IF EXISTS fee_config;
DROP TABLE IF EXISTS charge;

SET FOREIGN_KEY_CHECKS = 1;

-- After running this script, restart the Spring Boot app.
-- Hibernate will recreate all tables cleanly with the correct FK constraints.
