-- PRECONDITION: verified full backup and explicit production migration approval.
-- Pause activity/signup/card/payment writes while converting these legacy tables.
-- Recheck SHOW TABLE STATUS and exact row counts before and after each conversion.
-- Logical rows and column definitions are retained; physical table rebuilds are not transactional.
ALTER TABLE app_activity ENGINE=InnoDB;
ALTER TABLE app_activity_order ENGINE=InnoDB;
ALTER TABLE app_user_card ENGINE=InnoDB;
ALTER TABLE app_user_gold_log ENGINE=InnoDB;
