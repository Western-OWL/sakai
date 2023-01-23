----------------------------------------------------------------------
-- STEP 2: MySQL conversion script for OWL 20.2-owl6.1 to 22.3-owl1 --
----------------------------------------------------------------------

-- You must run the poll order backfill job before running this script --
ALTER TABLE POLL_OPTION MODIFY OPTION_ORDER INT NOT NULL;

-- once the SAK-46178 conversion is run successfully then the following tables can be dropped
-- DROP TABLE rbc_criterion_ratings;
-- DROP TABLE rbc_rubric_criterions;
-- OWL: we will instead just rename them
RENAME TABLE rbc_criterion_ratings to rbc_criterion_ratings_old;
RENAME TABLE rbc_rubric_criterions to rbc_rubric_criterions_old;

