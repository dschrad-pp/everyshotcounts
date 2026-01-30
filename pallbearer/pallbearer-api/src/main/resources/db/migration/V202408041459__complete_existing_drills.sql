--
-- Any existing drills can go ahead and get a COMPLETE drill status. This is
-- part of the refactor to move away from detecting drill status to having an
-- explicit status that is managed as part of the submission and pipeline
-- processes
--

-- UPDATE t_drill
--    SET drill_status = 'COMPLETE';