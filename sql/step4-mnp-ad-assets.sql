-- ============================================================
-- MNP ad positions for backend image upload (safe to re-run)
--
-- SQLyog tips:
-- 1. Run ONE INSERT block at a time (select lines -> Execute)
-- 2. Skip if you see: Duplicate entry
-- 3. Run verify SELECT at the end
--
-- position_code map:
--   mnp_brand_logo        home + service navbar logo
--   mnp_home_housekeeper  home page housekeeper avatar
--   mnp_profile_steward   my page housekeeper portrait
-- ============================================================

INSERT INTO app_ad_position (position_name, position_code, create_time, status)
SELECT 'MNP Brand Logo', 'mnp_brand_logo', NOW(), '0'
WHERE NOT EXISTS (
    SELECT 1 FROM app_ad_position WHERE position_code = 'mnp_brand_logo'
);

INSERT INTO app_ad_position (position_name, position_code, create_time, status)
SELECT 'MNP Home Housekeeper', 'mnp_home_housekeeper', NOW(), '0'
WHERE NOT EXISTS (
    SELECT 1 FROM app_ad_position WHERE position_code = 'mnp_home_housekeeper'
);

INSERT INTO app_ad_position (position_name, position_code, create_time, status)
SELECT 'MNP Profile Steward', 'mnp_profile_steward', NOW(), '0'
WHERE NOT EXISTS (
    SELECT 1 FROM app_ad_position WHERE position_code = 'mnp_profile_steward'
);

-- verify (read-only)
SELECT position_id, position_name, position_code, status
FROM app_ad_position
WHERE position_code IN ('mnp_brand_logo', 'mnp_home_housekeeper', 'mnp_profile_steward')
ORDER BY position_id;
