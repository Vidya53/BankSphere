-- ============================================================================
-- One-time schema fix for identity_service_db.users.role
--
-- ISSUE: The column was originally created when the Role enum only had short
-- values (CUSTOMER / CSR / ADMIN). After we added LOAN_OFFICER, BRANCH_MANAGER
-- and COMPLIANCE_OFFICER, MySQL began rejecting inserts with
-- "Data truncated for column 'role' at row 1" because Hibernate's
-- `ddl-auto: update` never widens existing columns.
--
-- The longest enum value is COMPLIANCE_OFFICER (18 chars). VARCHAR(32) leaves
-- headroom for any future role additions without another migration.
--
-- HOW TO RUN: open the MySQL CLI (or Workbench) connected to identity_service_db
-- and execute the statement below. No service restart is required after.
-- ============================================================================

USE identity_service_db;

ALTER TABLE users
    MODIFY COLUMN role VARCHAR(32) NOT NULL;

-- Verify (optional):
-- SHOW COLUMNS FROM users LIKE 'role';
