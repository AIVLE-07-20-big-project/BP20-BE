-- AI pipeline relational constraints.
-- Run against the shared BP20 MySQL database after the AI tables exist.

-- A job may exist before analysis_id is assigned, so the column remains nullable.
ALTER TABLE ai_analysis_jobs
    ADD CONSTRAINT fk_ai_jobs_analysis
    FOREIGN KEY (analysis_id) REFERENCES ai_analyses (analysis_id);
