-- Current production-backed image fixtures required by the mini program E2E environment.
-- This file is only applied to the local yixianghui_e2e database.

INSERT INTO app_ad_position (
    position_id, position_name, position_code, create_time, status
) VALUES
    (6, '首页热门地区', '0', '2025-09-14 15:30:00', '0'),
    (7, '客服列表', '0', '2026-04-29 14:48:11', '0'),
    (8, 'Logo', 'mnp_brand_logo', '2026-07-10 10:57:16', '0'),
    (9, '首页“小管家”图片', 'mnp_home_housekeeper', '2026-07-10 10:57:16', '0'),
    (10, '我的“小管家”图片', 'mnp_profile_steward', '2026-07-10 10:57:16', '0')
ON DUPLICATE KEY UPDATE
    position_name = VALUES(position_name),
    position_code = VALUES(position_code),
    create_time = VALUES(create_time),
    status = VALUES(status);

INSERT INTO app_ad_content (
    content_id, position_id, ad_name, description, ad_image, ad_content,
    start_time, end_time, link_url, order_num, status
) VALUES
    (14, 6, '昆明', '关联商品分类', '/profile/e2e/city-kunming-landmark.jpg', NULL, NULL, NULL, '38', 5, '1'),
    (15, 6, '建水', '关联商品分类；图片：建水朝阳楼，作者“瑞丽江的河水”，Wikimedia Commons，CC BY-SA 4.0，已裁剪。来源：https://commons.wikimedia.org/wiki/File:建水朝阳楼_-_2025-05-04.jpg', '/profile/e2e/city-jianshui-landmark.jpg', NULL, NULL, NULL, '56', 1, '1'),
    (16, 6, '腾冲', '关联商品分类\n', '/profile/e2e/city-tengchong-landmark.jpg', NULL, NULL, NULL, '', 3, '1'),
    (17, 6, '曲靖', '关联商品分类\n', '/profile/e2e/city-qujing-landmark.jpg', NULL, NULL, NULL, '27', 4, '1'),
    (18, 6, '全国', '关联商品分类', '/profile/upload/2025/09/14/全国_20250914161325A013.png', NULL, NULL, NULL, '25', 2, '1'),
    (19, 3, '二维码', '0871-65658585', '/profile/upload/2026/06/18/二维码_20260618112041A001.jpg', NULL, NULL, NULL, '', 1, '1'),
    (20, 7, '客服1', '18512345678', '/profile/upload/2026/06/16/二维码_20260616134125A004.jpg', '33333', NULL, NULL, '', 1, '1'),
    (21, 7, '客服2', '15812345678', '/profile/upload/2026/06/16/二维码_20260616134504A005.jpg', '123123', NULL, NULL, '', 1, '1'),
    (22, 3, '顶部背景图', '', '/profile/upload/2026/04/29/123123123_20260429145925A104.png', NULL, NULL, NULL, '', 1, '1'),
    (23, 6, '大理', '关联商品分类', '/profile/e2e/city-dali-landmark.jpg', NULL, NULL, NULL, '39', 4, '1'),
    (24, 8, '', '', '/profile/e2e/brand-logo-transparent.png', '', NULL, NULL, '', NULL, '1'),
    (25, 9, '', '', '/profile/upload/2026/07/27/图片人像抠图-removebg-prev_20260727115236A014.png', '', NULL, NULL, '', NULL, '1'),
    (26, 10, '', '', '/profile/upload/2026/07/27/图片人像抠图-removebg-prev_20260727115138A013.png', '', NULL, NULL, '', NULL, '1')
ON DUPLICATE KEY UPDATE
    position_id = VALUES(position_id),
    ad_name = VALUES(ad_name),
    description = VALUES(description),
    ad_image = VALUES(ad_image),
    ad_content = VALUES(ad_content),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    link_url = VALUES(link_url),
    order_num = VALUES(order_num),
    status = VALUES(status);
