-- Production product snapshot for the local mini program E2E database.
-- Source: https://shzxj.lk01.cn/api/mnp/index
-- Captured: 2026-08-04T16:54:14.194Z
-- This fixture intentionally replaces the legacy local product mocks.

SET NAMES utf8mb4;

DROP TABLE IF EXISTS app_goods_related;
DROP TABLE IF EXISTS app_goods_education_ext;
DROP TABLE IF EXISTS app_goods_sku_option;
DROP TABLE IF EXISTS app_goods_sku;
DROP TABLE IF EXISTS app_goods_sku_data;

CREATE TABLE app_goods_related (
    id bigint NOT NULL AUTO_INCREMENT,
    goods_id bigint NOT NULL,
    section_id varchar(100) NOT NULL,
    section_name varchar(100) NOT NULL,
    content text,
    sort_order int DEFAULT 0,
    min_content_length int DEFAULT 500,
    create_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_education_ext (
    ext_id bigint unsigned NOT NULL AUTO_INCREMENT,
    goods_id bigint unsigned NOT NULL,
    course_time varchar(100) DEFAULT '',
    course_place varchar(255) DEFAULT '',
    teacher_name varchar(100) DEFAULT '',
    lesson_count int unsigned DEFAULT NULL,
    class_size_max int unsigned DEFAULT NULL,
    class_size_min int unsigned DEFAULT NULL,
    start_date date DEFAULT NULL,
    signup_start date DEFAULT NULL,
    signup_end date DEFAULT NULL,
    material_note varchar(255) DEFAULT '',
    consult_phone varchar(30) DEFAULT '',
    create_time datetime DEFAULT CURRENT_TIMESTAMP,
    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ext_id),
    UNIQUE KEY uk_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_sku (
    sku_id bigint NOT NULL AUTO_INCREMENT,
    goods_id bigint NOT NULL,
    sku_name varchar(100) NOT NULL,
    sku_type varchar(3) NOT NULL DEFAULT '',
    sku_code varchar(20) NOT NULL DEFAULT '',
    par_sku_id bigint NOT NULL DEFAULT 0,
    sort_order int DEFAULT 0,
    status char(1) DEFAULT '1',
    valid_time datetime DEFAULT NULL,
    invalid_time datetime DEFAULT NULL,
    create_time datetime DEFAULT CURRENT_TIMESTAMP,
    stock int DEFAULT NULL,
    stock_unit varchar(20) DEFAULT NULL,
    sale_num int DEFAULT NULL,
    price decimal(10,2) DEFAULT NULL,
    sale_price decimal(10,2) DEFAULT NULL,
    PRIMARY KEY (sku_id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_sku_option (
    option_id bigint unsigned NOT NULL AUTO_INCREMENT,
    goods_id bigint unsigned DEFAULT 0,
    sku_id bigint DEFAULT NULL,
    option_name varchar(255) NOT NULL DEFAULT '',
    option_param varchar(255) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    status char(1) DEFAULT '0',
    option_type varchar(3) NOT NULL DEFAULT '',
    option_value varchar(1000) NOT NULL DEFAULT '',
    option_value_unit varchar(20) NOT NULL DEFAULT '',
    option_sort int unsigned DEFAULT 0,
    sku_seq_no int DEFAULT 0,
    PRIMARY KEY (option_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_goods_sku_data (
    data_id bigint unsigned NOT NULL AUTO_INCREMENT,
    goods_id bigint unsigned DEFAULT 0,
    sku_ids varchar(255) DEFAULT NULL,
    option_ids varchar(255) DEFAULT NULL,
    data_values varchar(255) DEFAULT NULL,
    data_price decimal(10,2) DEFAULT NULL,
    data_image varchar(255) DEFAULT '',
    data_stock bigint DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    status char(1) DEFAULT '0',
    PRIMARY KEY (data_id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELETE FROM app_goods;
DELETE FROM app_goods_category;
DELETE FROM app_goods_cart;
DELETE FROM app_goods_collect;
DELETE FROM app_goods_comment;

INSERT INTO app_goods_category (category_id, parent_id, parent_ids, category_name, category_icon, is_hot, link_type, link_id, remark, order_num, status) VALUES
    (40, 31, NULL, '精神慰藉服务', '', '0', 'goods', '0', '', 1, '1'),
    (49, 36, NULL, '口腔检查', '', '0', 'goods', '0', '', 1, '1'),
    (48, 35, NULL, '养老院', '', '0', 'goods', '0', '', 1, '1'),
    (47, 35, NULL, '护理院', '', '0', 'goods', '0', '', 1, '1'),
    (46, 35, NULL, '养老社区', '', '0', 'goods', '0', '', 1, '1'),
    (45, 35, NULL, '医养结合', '', '0', 'goods', '0', '', 1, '1'),
    (44, 31, NULL, '助洁', '', '0', 'goods', '0', '', 1, '1'),
    (43, 31, NULL, '助急', '', '0', 'goods', '0', '', 1, '1'),
    (42, 31, NULL, '助医', '', '0', 'goods', '0', '', 1, '1'),
    (41, 31, NULL, '上面护理', '', '0', 'goods', '0', '', 1, '1'),
    (50, 36, NULL, '眼科检查', '', '0', 'goods', '0', '', 1, '1'),
    (38, 25, NULL, '昆明', '', '1', 'goods', '0', '', 1, '1'),
    (34, 31, NULL, '代办服务', '/profile/upload/2025/03/31/avatar_empty_20250331184118A006.png', '0', 'goods', '0', '', 1, '1'),
    (51, 36, NULL, '健康/慢病管理', '', '0', 'goods', '0', '', 1, '1'),
    (52, 37, NULL, '线上课程', '', '0', 'goods', '0', '', 1, '1'),
    (53, 37, NULL, '线下课程', '', '0', 'goods', '0', '', 1, '1'),
    (59, 58, '0,58', '健康', '', '0', 'goods', '0', '', 1, '1'),
    (25, 0, NULL, '全国旅居', '/profile/upload/2025/10/26/逸享荟小程序-04_20251026230816A002.png', '1', 'goods', '0', '纯玩康养游', 1, '1'),
    (27, 25, NULL, '曲靖', '', '0', 'goods', '0', '', 2, '1'),
    (60, 58, '0,58', '手机', '', '0', 'goods', '0', '', 2, '1'),
    (30, 26, NULL, '逸享管家', '', '0', 'goods', '0', '', 2, '1'),
    (29, 26, NULL, '家政服务', '', '0', 'goods', '0', '', 2, '1'),
    (58, 0, '0', '老年教育', '', '0', 'goods', '0', '', 3, '1'),
    (28, 25, NULL, '腾冲', '', '0', 'goods', '0', '', 3, '1'),
    (61, 58, '0,58', '兴趣', '', '0', 'goods', '0', '', 3, '1'),
    (39, 25, NULL, '大理', '', '0', 'goods', '0', '', 4, '1'),
    (32, 0, NULL, ' 聚会活动', '/profile/upload/2025/10/26/逸享荟小程序-06_20251026230946A005.png', '0', 'activity', '0', '活动分类', 4, '1'),
    (54, 25, NULL, '丽江', '', '0', 'goods', '0', '', 5, '1'),
    (55, 25, NULL, '芒市', '', '0', 'goods', '0', '', 6, '1'),
    (56, 25, NULL, '建水', '', '0', 'goods', '0', '', 7, '1'),
    (57, 25, NULL, '弥勒', '', '0', 'goods', '0', '', 8, '1');

INSERT INTO app_goods (goods_id, category_id, category_ids, dept_id, goods_name, goods_cover, goods_images, description, tags, price, vip_price, unit, specifications, stock, goods_type, is_top, is_hot, attr_ids, attr_values, is_sku, award_type, award_parent_ratio, award_grand_parent_ratio, award_golden, content, express_fee, weight, view_count, sale_count, create_time, update_time, status) VALUES
    (38, 61, '0', 102, '水彩绘画', '/profile/upload/2026/07/10/entry-education-bg-g_20260710111142A004.jpg', '/profile/upload/2026/07/10/entry-education-bg-g_20260710111145A005.jpg', '10节水彩基础画课程，涵盖风景、花卉和小品，包含技法示范、学员练习和整体点评。', '小班授课|10次课|≤18人', 1, 1, '1期', NULL, 17, 'education', 0, 1, NULL, NULL, 0, '0', NULL, NULL, NULL, NULL, 0, NULL, 0, 1, '2026-07-10 11:15:36', '2026-07-29 18:46:00', '1'),
    (37, 39, '0', 102, '大理一号基地（洱海才村码头）', '/profile/upload/2026/07/01/IMG_5293_20260701152916A006.JPG', '/profile/upload/2026/07/01/IMG_5293_20260701152942A007.JPG', '大理洱海基地位于繁华的才村，这里距离大理古城3公里，有直达公交车，距离洱海边300米，这里有超市，饭店，药房，菜市场，公交车，出租车，小黄车，交通生活都十分便利！', '舒适|美食', 1, NULL, NULL, NULL, NULL, 'hotel', 0, 1, NULL, NULL, 1, '0', NULL, NULL, NULL, '<p><span style="background-color: rgb(251, 245, 203);">大理洱海基地价格如下：</span></p><p><span style="background-color: rgb(251, 245, 203);">普通房间（标间/大床）</span></p><p>拼房（两人一间）每人：</p><p>745元/7天；2980元/30天</p><p>包房（一人一间）：</p><p>1070元/7天；3680元/30天</p><p>普通房间暑假期间（7月10日至8月20日）价格上浮：60元/间/天</p><p>国庆上浮：100元/间/天</p><p>春节普通房间价格上浮：100/间/天</p><p>住满一个月春节不涨价</p><p><span style="background-color: rgb(251, 245, 203);">豪华房间（标间/大床/亲子间/套房）</span></p><p>拼房（两人一间）每人：</p><p>885元/7天；3580元/30天</p><p>包房（一人一间）：</p><p>1235元/7天；4580元/30天</p><p>五一（5.1-5.2）每天每间房+200</p><p>豪华房间暑假期间（7月10日至8月20日）价格上浮：100元/间/天</p><p>国庆上浮：200元/间/天</p><p>春节豪华房间价格上浮200元/间/天</p><p>住满一个月春节不涨价</p><p><br></p>', 0, NULL, 0, 0, '2026-07-01 15:31:39', NULL, '1'),
    (32, 38, '0', 108, '昆明古滇基地', '/profile/upload/2026/04/28/head_20260428225023A065.jpg', '/profile/upload/2026/04/28/intro1_20260428230138A068.jpg,/profile/upload/2026/04/28/intro2_20260428230142A069.jpg,/profile/upload/2026/04/28/intro3_20260428230146A070.jpg', '坐落于昆明市晋宁区环湖南路北段七彩云南古滇名城内，距离昆明主城区约35公里，距离昆明新南站（高铁站）20公里，离昆明长水国际机场约60公里，距古滇湿地公园2.3公里。', '商圈|购物|美食', 990, NULL, NULL, NULL, NULL, 'hotel', 0, 1, NULL, NULL, 1, '0', NULL, NULL, NULL, '<p>基地总占地面积12000亩，内设置康养公寓、滇池老年大学、滇池康悦医院、旅居食堂、诺享生活超市、美食广场、怡心园、麦当劳、水景广场等生活服务空间，无障碍通道将居住、休闲、娱乐、餐饮、医养等日常生活融于一体，保障入住客户生活的舒适度与便利性。</p><p>基地内老年大学，拥有俱乐部、小剧场、图书馆、健身房、乒乓球室、桌球室、IT教室、手工教室、国艺坊、书法教室、西洋画教室、舞蹈教室、声乐教室、乐器教室等众多功能与娱乐休闲区应有尽有，11个社团活动可同时容纳 500 余人开展活动。</p>', 0, NULL, 0, 0, '2026-04-28 09:53:34', NULL, '1'),
    (31, 38, '0', 108, '昆明六号温泉基地', '/profile/upload/2026/04/28/navpage_20260428231031A077.jpg', '/profile/upload/2026/04/28/enviroment1_20260428231041A078.jpg,/profile/upload/2026/04/28/environment2_20260428231044A079.jpg,/profile/upload/2026/04/28/environment3_20260428231047A080.jpg,/profile/upload/2026/04/28/environment4_20260428231049A081.jpg', '昆明六号温泉基地拥有纯朴自然的风光，得天独厚的地理优势，保留了历史文化遗产茶马古道，整体装修采用独特的极简设计，向人们展现着舒适惬意的治愈系生活方式。一直以来，基地都倡导人们亲近自然，热爱简单纯粹的生活，珍惜每个与家人相处陪伴的时刻。在这里，可以享受着温泉带来的安逸偷悦，感受着基地设计中的康养美学。基地位于交通优势距昆明16公里，距富民县城5公里紧邻昆禄公路（108国道）及京昆高速公路毗临万亩大盘的天马山温泉旅游度假区依托优良区位交通优势以「温泉慢生活」为线索构建“自然+生态+文化+康养”的组合模式打造集文化体验、温泉舒缓、亲近自然于一体的自然舒缓体验游。', '舒适|美食', 1080, NULL, NULL, NULL, NULL, 'hotel', 0, 1, NULL, NULL, 1, '0', NULL, NULL, NULL, '<p><br></p>', 0, NULL, 0, 0, '2026-04-27 22:12:25', NULL, '1');

INSERT INTO app_goods_related (id, goods_id, section_id, section_name, content, sort_order, min_content_length, create_time) VALUES
    (316, 38, 'course_content', '课程内容', '<p><span style="color: rgb(77, 77, 77);">通过10节课练习后，可对水彩画风景类有基本认识，并能熟练完成简单风景。</span></p><p><br></p><p><span style="color: rgb(119, 119, 119);">每门课程每周一次，一学期共10次课。线下授课，小班授课（≤18人）。招满10人开班，2026年9月开课。</span></p>', 1, 250, '2026-07-29 14:26:07'),
    (317, 38, 'signup_info', '报名信息', '<ol><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">招生对象：年龄在30-75周岁，身体健康并能自主学习的市民均可报名。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">报名时间：2026年06月23日起至课程开课。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">缴费方式：报名一周内至宝山区共江路660号邻聚乐学驿站现场缴纳现金或者微信支付。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">咨询电话：13764363947，工作日9:00-12:00，13:30-17:00。</span></li></ol><p><br></p>', 2, 250, '2026-07-29 14:26:07'),
    (318, 38, 'signup_notice', '报名须知', '<ol><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">缴费后方可算报名成功，未按时缴费视为自动放弃报名资格，学校不再保留名额。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">报名缴费成功后一般不允许换班、顶班，不得自行调班或串班听课。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">请假仍扣课时，不退费、不补课、不延续。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><span style="color: rgb(77, 77, 77);">正式录取后原则上不再办理退课、退费、转班；特殊情况需按学校要求提交证明材料。</span></li></ol><p><br></p>', 3, 250, '2026-07-29 14:26:07'),
    (321, 37, 'basic', '基本特色', '<p>❗❗❗在大理，我们一共有4个院子，分别是一号院（壹号院是我们的普通房间➕一楼是餐厅，二三楼是客房）</p><p><br></p><p>‼‼二号院，三号院，五号院（是我们的豪华房间，需要步行2～5分钟到一号院餐厅吃饭）</p><p><br></p><p><span style="background-color: rgb(217, 234, 252);">4个院子分别有什么不同？‼‼‼</span></p><p>平时一日三餐，都在一号院吃饭，五号院和二号院，都有独立的洗衣机和冰箱，厨房，厨具，餐具🍽全部配备齐全！（可以偶尔自己做饭！）三号院部分房型没有洗衣机和冰箱，厨房，厨具，餐具。</p><p><br></p><p>我们每个院子的地理位置都非常好，交通，位置，别的都差不多！去洱海边步行只要3-5分钟😊</p><p><br></p><p>请注意：两人一间价格×2</p><p>一人一间是包房，是指一人住一间</p><p><br></p><p>❗儿童政策：3岁以下免费，3-7岁20元/天餐费，8岁以上按照成人收费。</p><p>（两成人带8岁以上儿童住一间，儿童收费60元餐费）</p><p><br></p><p>附近景点有；大理古城，崇圣寺三塔5A级，张家花园3A级，蝴蝶泉公园4A级，杨丽萍大剧院，寂照庵，磻溪村s湾，龙龛码头，洱海生态廊道，苍山共有三条索道，1感通索道，2洗马谭索道，3中和索道，沙溪古镇，喜洲古镇，双廊古镇，杨丽萍太阳宫等等……</p><p><img src="/api/profile/upload/2026/07/02/IMG_5295_20260702140107A016.JPG"></p><p>距离大理火车站19公里，距离大理机场35公里左右，专车接站自费，💰60一辆车（可以坐4人），接机💰100一辆车！</p><p><img src="/api/profile/upload/2026/07/02/IMG_5298_20260702140148A017.JPG"></p><p><br></p><p><br></p><p><br></p>', 1, 250, '2026-07-29 18:12:41'),
    (322, 37, 'policy', '政策', '<p><span style="color: rgb(38, 38, 38);">住宿床位/一日三餐/文体娱乐活动/水电网洗衣机等生活设施/房间保洁（1次/周）/基地定期福利活动/基地管家及出行向导/其他配套设施/大理及周边游活动（AA制）。</span></p>', 2, 250, '2026-07-29 18:12:41'),
    (270, 32, 'basic', '基本特色', '<p><span style="background-color: rgb(245, 212, 128);">🏡🏡【房型信息】</span></p><p>⛲⛲两室一厅一厨一卫精装修公寓，宽敞明亮；两种装修风格可选择（欧式/中式），一样的精装不同的体验。</p><p><br></p><p>🏮🏮公寓酒店套房</p><p>楼层位置：1-7层</p><p>房间面积：85平方米</p><p>床型：2张1.5米双人床（可住4人）</p><p>房间配置：客厅｜厨房｜有窗｜餐具（不含餐具）|洗衣机｜水电气能源</p><p><img src="/api/profile/upload/2026/04/28/room1_20260428225414A067.jpg"><img src="/api/profile/upload/2026/04/28/room2_20260428230220A071.jpg"><img src="/api/profile/upload/2026/04/28/room3_20260428230233A072.jpg"><img src="/api/profile/upload/2026/04/28/room4_20260428230245A073.jpg"></p><p>🏰🏰欧式客房</p><p>楼层位置：1-7层</p><p>房间面积：85平方米</p><p>床型：2张1.5米双人床（可住4人）</p><p>房间配置：客厅｜厨房｜有窗｜厨具：炒锅*1、汤锅*1、电饭煲*1（不含餐具）|洗衣机｜水电气能源</p><p><img src="/api/profile/upload/2026/04/28/room5_20260428230312A074.jpg"><img src="/api/profile/upload/2026/04/28/room6_20260428230323A075.jpg"><img src="/api/profile/upload/2026/04/28/room7_20260428230340A076.jpg"></p><p><span style="background-color: rgb(245, 212, 128);">🪁🪁【基地活动】</span></p><p>基地内有：俱乐部、小剧场、图书馆健身房、乒乓球室、桌球室、IT教室手工教室、国艺坊、书法教室、西洋画教室、舞蹈教室、声乐教室、乐器教室等众多功能与娱乐休闲区，满足不同爱好与需求，还能结交四方好友。</p><p><img src="/api/profile/upload/2026/04/28/book1_20260428110114A033.jpg"></p><p><img src="/api/profile/upload/2026/04/28/book2_20260428110146A034.jpg"><img src="/api/profile/upload/2026/04/28/liter1_20260428110225A035.jpg"></p><p><span style="background-color: rgb(245, 212, 128);">🥁🥁【特色活动】</span></p><p>可参加老年大学活动：联谊活动、歌唱活动、文艺比赛等（根据社区课程活动日历安排提前预约报名参与）。</p><p><img src="/api/profile/upload/2026/04/28/liter2_20260428110433A036.jpg"><img src="/api/profile/upload/2026/04/28/liter3_20260428110446A037.jpg"></p><p><span style="background-color: rgb(245, 212, 128);">付费参与活动如下：</span></p><p><span style="background-color: rgb(245, 212, 128);">⬇️⬇️⬇️单项付费参加康养活动：</span></p><p>每周二外出活动30元/人/次；</p><p>生日会50元/人/次；</p><p>节庆活动100元/人/次；</p><p>每月1次生日PARTY，联动主题/节日活动。</p><p><img src="/api/profile/upload/2026/04/28/liter4_20260428110515A038.jpg"><img src="/api/profile/upload/2026/04/28/liter5_20260428110538A039.jpg"></p><p>🚌🚌根据出游日历表安排提前预约报名参与</p><p><img src="/api/profile/upload/2026/04/28/liter6_20260428110552A040.jpg"></p>', 1, 250, '2026-06-15 17:09:23'),
    (271, 32, 'food', '餐饮', '<p><span style="background-color: rgb(245, 212, 128);">🍜🍜【餐食安排】</span></p><p>——餐卡储值</p><p>用餐方式灵活，可以按需使用餐饮储值卡在餐厅或者美食街刷卡用餐；也可以自己做饭，房间配套厨房，为您提供最大程度的便捷。</p><p>🍽🍽餐饮储值卡通用美食广场、怡心园、诺享生活超市。</p><p>基地内包含旅居餐厅，以及特色美食广场怡心园、麦当劳等餐厅，方便用餐。美食广场11个美食档口汇聚天南海北的特色美食满足各地口味需求，以及口味变化需求。</p><p>旅居餐厅菜品均为称重4.18元/100g，也有单点菜品可供选择</p><p><img src="/api/profile/upload/2026/04/28/food1_20260428111159A041.jpg"><img src="/api/profile/upload/2026/04/28/food2_20260428111233A042.jpg"><img src="/api/profile/upload/2026/04/28/food3_20260428111311A044.jpg"><img src="/api/profile/upload/2026/04/28/food4_20260428111331A045.jpg"><img src="/api/profile/upload/2026/04/28/food5_20260428111346A046.jpg"></p>', 2, 250, '2026-06-15 17:09:23'),
    (272, 32, 'medical', '医疗', '<p><span style="background-color: rgb(245, 212, 128);">🏥🏥【安全保障服务】</span></p><p>👩‍💼👩‍💼管家服务——紧急呼叫+24小时专人值守</p><p>💊💊医疗配套——基地内专设滇池康悦医院，有门诊及住院部，小病慢病不出门，大病急诊有通道。</p><p><img src="/api/profile/upload/2026/04/28/health1_20260428111538A047.jpg"><img src="/api/profile/upload/2026/04/28/health2_20260428111546A048.jpg"><img src="/api/profile/upload/2026/04/28/health3_20260428111555A049.jpg"></p>', 3, 250, '2026-06-15 17:09:23'),
    (273, 32, 'shopping', '购物', '<p>🏢基地内：配套有诺享生活超市，生活用品、蔬果饮品、休闲食品、云南特色等分区各类商品一应俱全，一站式打包生活所需物资。</p><p><img src="/api/profile/upload/2026/04/28/market1_20260428111905A050.jpg"></p>', 4, 250, '2026-06-15 17:09:23'),
    (274, 32, 'attractions', '周边景点', '<p>🏙基地周边：附近有七彩·云南欢乐世界、古滇温泉山庄、古滇朵拉萌宠乐园、古滇精品湿地等。</p><p><img src="/api/profile/upload/2026/04/28/neibor1_20260428111954A051.jpg"><img src="/api/profile/upload/2026/04/28/neibor2_20260428112008A052.jpg"></p>', 5, 250, '2026-06-15 17:09:23'),
    (275, 32, 'traffic', '交通', '<p>基地位置：坐落于昆明市晋宁区环湖南路北段七彩云南古滇名城内，距离昆明主城区约35公里，距离昆明新南站（高铁站）20公里，离昆明长水国际机场约60公里，距古滇湿地公园2.3公里。</p><p>🌅其他：距离古滇湿地公园2.3公里，饭后散步至滇池边约30分钟，车程约8分钟。</p>', 6, 250, '2026-06-15 17:09:23'),
    (276, 32, 'policy', '政策', '<p><span style="background-color: rgb(245, 212, 128);">【产品价格详情】</span><img src="/api/profile/upload/2026/04/28/price_20260428112617A053.jpg"></p><p><span style="color: rgb(223, 42, 63);">高峰加价时段是：5月1日至5月4日、7月15日至8月20日、10月1日至10月6日、春节初一至初六。</span></p><p><span style="color: rgb(223, 42, 63);">加价规则是：周旅居每晚加价120元/天/间、半月旅居加价100元/天/间、月度旅居加价60元/天/间，半年和年度长租不加价。</span></p><p><span style="background-color: rgb(245, 212, 128);">💫💫【费用包含】</span></p><p>基地住宿费</p><p>提供用水，用电、煤气及热水等基础生活配套，房费包含水电等能源费用</p><p>免费接驳电瓶车，可乘坐直达各楼栋</p><p>新东方定制保险</p><p>每周打扫一次卫生，每半月换洗一次床上布草；</p><p><span style="background-color: rgb(245, 212, 128);">📝📝【预定规则】</span></p><p>1、全款支付预约</p><p>2、入住前需支付一套房押金3000元，退住即退还；</p><p>3、入住时间：即到即入住；</p><p>4、离店时间：当天离店即可</p><p>5、退改签政策：出行前7天无损取消；入住后，除不可抗力等因素外，不予退费；</p><p><br></p>', 7, 250, '2026-06-15 17:09:23'),
    (323, 31, 'basic', '基本特色', '<p>基地为了创造这种优质的温泉体验，将天富民地区最好的温泉水送到每间汤屋的泡池，以及散落在山顶的温泉池内。这里的温泉水属于复合型理疗价值的热矿水，水温常年保持在40℃左右，温泉水中偏硅酸含量57mg/L，具有软化血管的功能，对动脉硬化、心血管疾病等能起到缓解作用。拥有28个功能各异、大小不一的温泉泡池、儿童戏水池1个、成人游泳池1个、儿童游泳池1个。</p><p><img src="/api/profile/upload/2026/04/28/enviroment1_20260428231402A082.jpg"><img src="/api/profile/upload/2026/04/28/environment2_20260428231407A083.jpg"><img src="/api/profile/upload/2026/04/28/environment3_20260428231411A084.jpg"><img src="/api/profile/upload/2026/04/28/environment4_20260428231415A085.jpg"></p><p><strong>客房住宿299间：</strong></p><p>一号楼 标间61间、单间8间二号楼： 标间47间A1栋别墅：单间3间、标间1间 A2栋别墅：单间2间、标间2间 C栋别墅: 标间4间 D栋别墅: 标间4间E1栋别墅: 标间3间、单间2间E2栋别墅：标间3间、单间2间 E3栋别墅：标间2间 综合楼客房：单间10间，标间102间</p><p><span style="color: rgb(38, 38, 38);">F1栋别墅：单间1间F2栋别墅：单间1间1号汤屋：单间2间2号汤屋：单间2间3号汤屋：单间1间5号汤屋：单间1间室内私汤大床房：4间景观亲子房：8间景观大床房：3间普通客房：20间</span></p><p><img src="/api/profile/upload/2026/04/28/hall1_20260428231706A086.jpg"></p><p><span style="color: rgb(38, 38, 38);">豪华双床房（2张1.2米单人床，23-25㎡ 1-3层）</span></p><p><img src="/api/profile/upload/2026/04/28/room1_20260428231730A087.jpg"><img src="/api/profile/upload/2026/04/28/room2_20260428231749A088.jpg"></p><p><span style="color: rgb(38, 38, 38);">豪华大床房（1张2米特大床，45㎡ 1-3层）</span></p><p><img src="/api/profile/upload/2026/04/28/room3_20260428231834A090.png"></p><p><span style="color: rgb(38, 38, 38);">极具特色的户外森林温泉</span></p><p><img src="/api/profile/upload/2026/04/28/fea1_20260428232031A092.jpg"><img src="/api/profile/upload/2026/04/28/fea2_20260428232100A093.jpg"></p><p><strong style="color: rgb(38, 38, 38);">会堂：600人会议（配LED屏）</strong></p><p><img src="/api/profile/upload/2026/04/28/meet1_20260428232131A094.png"><img src="/api/profile/upload/2026/04/28/liter1_20260428232145A095.png"><img src="/api/profile/upload/2026/04/28/liter22_20260428232206A096.png"></p>', 1, 250, '2026-07-29 18:13:02'),
    (324, 31, 'food', '餐饮', '<p><strong style="color: rgb(38, 38, 38);">基地餐厅餐食</strong></p><p><img src="/api/profile/upload/2026/04/28/merchant_20260428231936A091.jpg"></p>', 2, 250, '2026-07-29 18:13:02'),
    (325, 31, 'policy', '政策', '<p><img src="/api/profile/upload/2026/04/28/policy_20260428232552A098.png"></p>', 3, 250, '2026-07-29 18:13:02');

INSERT INTO app_goods_education_ext (ext_id, goods_id, course_time, course_place, teacher_name, lesson_count, class_size_max, class_size_min, start_date, signup_start, signup_end, material_note, consult_phone, create_time, update_time) VALUES
    (1, 38, '周三9：00~10：30', '共江路666号', '周周', 10, 18, 10, '2026-08-01', '2026-07-23', '2026-07-30', '自备水彩笔', '13764363947', '2026-07-10 11:15:36', '2026-07-29 14:26:07');

INSERT INTO app_goods_sku (sku_id, goods_id, sku_name, sku_type, sku_code, par_sku_id, sort_order, status, valid_time, invalid_time, create_time, stock, stock_unit, sale_num, price, sale_price) VALUES
    (34, 37, '一号院', '200', '', '0', 1, '1', NULL, NULL, '2026-07-03 09:50:39', 10, '间', NULL, 1, NULL),
    (35, 37, '一号院大床房', '202', '', '34', 2, '1', NULL, NULL, '2026-07-03 09:53:23', 10, '间', NULL, 1, NULL),
    (13, 32, '旅居A套餐(含餐卡及能源】', '200', '', '0', 1, '1', NULL, NULL, '2026-04-28 13:25:57', 500, '间', NULL, 213, NULL),
    (18, 32, '旅居B套餐(不含餐)', '200', '', '0', 2, '1', NULL, NULL, '2026-04-28 15:12:17', 300, '间', NULL, 141, NULL),
    (19, 32, '租房C套餐(不含餐及能源)', '200', '', '0', 3, '1', NULL, NULL, '2026-04-28 15:12:17', 300, '间', NULL, 424, NULL),
    (20, 32, '全包周旅居', '202', '', '13', 4, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间', NULL, 1490, NULL),
    (21, 32, '全包半月旅居', '202', '', '13', 5, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间', NULL, 2600, NULL),
    (22, 32, '全包月度旅居', '202', '', '13', 6, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间隔', NULL, 4930, NULL),
    (23, 32, '舒适周旅居', '202', '', '18', 7, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间', NULL, 990, NULL),
    (24, 32, '舒适半月旅居', '202', '', '18', 8, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间', NULL, 1800, NULL),
    (25, 32, '月度短租旅居', '202', '', '19', 9, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间', NULL, 2970, NULL),
    (26, 32, '半年短租旅居', '202', '', '19', 10, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间', NULL, 2700, NULL),
    (27, 32, '年度长租旅居', '202', '', '19', 11, '1', NULL, NULL, '2026-04-28 15:19:04', 100, '间', NULL, 2400, NULL),
    (28, 31, '两人一间（每人）', '200', '', '0', 1, '1', NULL, NULL, '2026-04-28 23:35:46', 50, '间', NULL, 180, NULL),
    (29, 31, '一人一间', '200', '', '0', 2, '1', NULL, NULL, '2026-04-28 23:35:46', 50, '间', NULL, 280, NULL),
    (30, 31, '8天7晚', '202', '', '28', 3, '1', NULL, NULL, '2026-04-28 23:35:46', 30, '间', NULL, 1080, NULL),
    (31, 31, '31天30晚', '202', '', '28', 4, '1', NULL, NULL, '2026-04-28 23:35:46', 20, '间', NULL, 4280, NULL),
    (32, 31, '8天7晚', '202', '', '29', 5, '1', NULL, NULL, '2026-04-28 23:45:19', 30, '间', NULL, 1680, NULL),
    (33, 31, '31天30晚', '202', '', '29', 6, '1', NULL, NULL, '2026-04-28 23:51:37', 20, '间', NULL, 6480, NULL);

INSERT INTO app_goods_sku_option (option_id, goods_id, sku_id, option_name, option_param, create_time, status, option_type, option_value, option_value_unit, option_sort, sku_seq_no) VALUES
    (104, 37, 34, '大床房', NULL, '2026-07-03 10:02:15', '1', '305', '/profile/upload/2026/07/03/IMG_5260_20260703095741A001.JPG', '', 1, 1),
    (105, 37, 34, '介绍', NULL, '2026-07-03 10:02:15', '1', '304', '一号院房间👇👇👇', '', 2, 1),
    (106, 37, 35, '天数', NULL, '2026-07-03 10:08:34', '1', '303', '7', '天', 1, 0),
    (107, 37, 35, '套餐', NULL, '2026-07-03 10:08:34', '1', '304', '周旅居为7天6晚', '', 2, 1),
    (108, 37, 35, '总价', NULL, '2026-07-03 10:08:34', '1', '302', '1', '元', 3, 1),
    (26, 32, 13, '套餐图片', NULL, '2026-04-28 15:27:13', '1', '305', '/profile/upload/2026/04/28/room1_20260428152201A061.jpg', '', 1, 1),
    (27, 32, 13, '介绍1', NULL, '2026-04-28 15:27:13', '1', '304', '·古滇度假酒店标两室一厅85m2公寓一套(可入住2-4人); ·提供用品:每套房厨具一套(不含餐具);', '', 2, 1),
    (28, 32, 13, '介绍2', NULL, '2026-04-28 15:27:13', '1', '304', '清洁服务:每周1次保洁服务，半月1次床上用品换洗; 其他费用:含物管和网络及水电气能源费;', '', 3, 1),
    (29, 32, 13, '介绍3', NULL, '2026-04-28 15:27:13', '1', '304', '古滇食堂通用餐饮储值卡:周旅居500元/套房、半月旅居800元/套房及月度旅居1600元/套房。', '', 4, 1),
    (45, 32, 18, '套餐图', NULL, '2026-04-28 16:36:44', '1', '305', '/profile/upload/2026/04/28/room2_20260428163245A062.jpg', '', 1, 2),
    (46, 32, 18, '介绍1', NULL, '2026-04-28 16:36:44', '1', '304', '古滇度假酒店标两室一厅85m公寓一套(可入住2-4人);', '', 2, 2),
    (47, 32, 18, '介绍2', NULL, '2026-04-28 16:36:44', '1', '304', '提供用品:每套房厨具一套(不含餐具);', '', 3, 2),
    (48, 32, 18, '·介绍3', NULL, '2026-04-28 16:36:44', '1', '304', '·清洁服务:每周1次保洁服务，半月1次床上用品换洗;', '', 4, 2),
    (49, 32, 18, '介绍4', NULL, '2026-04-28 16:36:44', '1', '304', '·其他费用:含物管和网络及水电气能源费。', '', 5, 2),
    (60, 32, 19, '套餐图', NULL, '2026-04-28 16:46:24', '1', '305', '/profile/upload/2026/04/28/room5_20260428164344A063.jpg', '', 1, 3),
    (61, 32, 19, '介绍1', NULL, '2026-04-28 16:46:24', '1', '304', '·古滇度假酒店标两室一厅85m2公寓一套(可入住2-4人);', '', 2, 3),
    (62, 32, 19, '介绍2', NULL, '2026-04-28 16:46:24', '1', '304', '提供用品:每套房厨具一套(不含餐具);', '', 3, 3),
    (63, 32, 19, '介绍3', NULL, '2026-04-28 16:46:24', '1', '304', '·清洁服务:每周1次保洁服务，半月1次床上用品换洗;', '', 4, 3),
    (64, 32, 19, '介绍4', NULL, '2026-04-28 16:46:24', '1', '304', '·其他费用:含物管和网络费。', '', 5, 3),
    (30, 32, 20, '天数', NULL, '2026-04-28 15:49:29', '1', '303', '7', '天', 1, 0),
    (31, 32, 20, '套餐定义1', NULL, '2026-04-28 15:49:29', '1', '304', '周旅居为7天6晚', '', 2, 1),
    (32, 32, 20, '总价', NULL, '2026-04-28 15:49:29', '1', '302', '1490', '元', 3, 1),
    (33, 32, 20, '均价', NULL, '2026-04-28 15:49:29', '1', '301', '212', '元', 4, 1),
    (34, 32, 20, '床位', NULL, '2026-04-28 15:49:29', '1', '303', '2', '床', 5, 1),
    (35, 32, 21, '天数', NULL, '2026-04-28 16:28:50', '1', '303', '31', '天', 1, 0),
    (36, 32, 21, '介绍', NULL, '2026-04-28 16:28:50', '1', '304', '半月旅居为16天15晚', '', 2, 1),
    (37, 32, 21, '总价', NULL, '2026-04-28 16:28:50', '1', '302', '2600', '元', 3, 1),
    (38, 32, 21, '均价', NULL, '2026-04-28 16:28:50', '1', '301', '371', '元', 4, 1),
    (39, 32, 21, '床位', NULL, '2026-04-28 16:28:50', '1', '303', '2', '床', 5, 1),
    (40, 32, 22, '天数', NULL, '2026-04-28 16:28:50', '1', '303', '31', '天', 1, 1),
    (41, 32, 22, '介绍', NULL, '2026-04-28 16:28:50', '1', '304', '月度旅居为31天30晚', '', 2, 1),
    (42, 32, 22, '总价', NULL, '2026-04-28 16:28:50', '1', '302', '4930', '元', 3, 1),
    (43, 32, 22, '均价', NULL, '2026-04-28 16:28:50', '1', '301', '704', '元', 4, 1),
    (44, 32, 22, '床位', NULL, '2026-04-28 16:28:50', '1', '303', '2', '床', 5, 1),
    (50, 32, 23, '天数', NULL, '2026-04-28 16:42:52', '1', '303', '7', '', 1, 0),
    (51, 32, 23, '介绍', NULL, '2026-04-28 16:42:52', '1', '304', '周旅居为7天6晚', '', 2, 2),
    (52, 32, 23, '总价', NULL, '2026-04-28 16:42:52', '1', '302', '990', '元', 3, 2),
    (53, 32, 23, '均价', NULL, '2026-04-28 16:42:52', '1', '301', '141', '元', 4, 2),
    (54, 32, 23, '床位', NULL, '2026-04-28 16:42:52', '1', '303', '2', '床', 5, 2),
    (55, 32, 24, '天数', NULL, '2026-04-28 16:42:52', '1', '303', '16', '天', 1, 0),
    (56, 32, 24, '介绍', NULL, '2026-04-28 16:42:52', '1', '304', '半月旅居为16天15晚', '', 2, 2),
    (57, 32, 24, '总价', NULL, '2026-04-28 16:42:52', '1', '302', '1800', '元', 3, 2),
    (58, 32, 24, '均价', NULL, '2026-04-28 16:42:52', '1', '301', '257', '元', 4, 2),
    (59, 32, 24, '床位', NULL, '2026-04-28 16:42:52', '1', '303', '2', '床', 5, 2),
    (65, 32, 25, '天数', NULL, '2026-04-28 16:54:54', '1', '303', '31', '天', 1, 0),
    (66, 32, 25, '介绍', NULL, '2026-04-28 16:54:54', '1', '304', '月度旅居为31天30晚', '', 2, 3),
    (67, 32, 25, '总价', NULL, '2026-04-28 16:54:54', '1', '302', '2970', '元', 3, 3),
    (68, 32, 25, '均价', NULL, '2026-04-28 16:54:54', '1', '301', '424', '元', 4, 3),
    (69, 32, 25, '床位', NULL, '2026-04-28 16:54:54', '1', '303', '2', '床', 5, 3),
    (70, 32, 26, '天数', NULL, '2026-04-28 16:54:54', '1', '303', '30', '天', 1, 0),
    (71, 32, 26, '介绍', NULL, '2026-04-28 16:54:54', '1', '304', '半年短期旅居按照30晚为1个月计算', '', 2, 3),
    (72, 32, 26, '总价', NULL, '2026-04-28 16:54:54', '1', '302', '2700', '元', 3, 3),
    (73, 32, 26, '均价', NULL, '2026-04-28 16:54:54', '1', '301', '386', '元', 4, 3),
    (74, 32, 26, '床位', NULL, '2026-04-28 16:54:54', '1', '303', '2', '床', 5, 3),
    (75, 32, 27, '天数', NULL, '2026-04-28 16:54:54', '1', '303', '30', '天', 1, 0),
    (76, 32, 27, '介绍', NULL, '2026-04-28 16:54:54', '1', '304', '年度长租旅居按照30晚为1个月计算', '', 2, 3),
    (77, 32, 27, '总价', NULL, '2026-04-28 16:54:54', '1', '302', '2400', '元', 3, 3),
    (78, 32, 27, '均价', NULL, '2026-04-28 16:54:54', '1', '301', '343', '元', 4, 3),
    (79, 32, 27, '床位', NULL, '2026-04-28 16:54:54', '1', '303', '2', '床', 5, 3),
    (80, 31, 28, '套餐图', NULL, '2026-04-28 23:35:46', '1', '305', '/profile/upload/2026/04/28/room1_20260428233339A099.jpg', '', 1, 1),
    (81, 31, 28, '介绍', NULL, '2026-04-28 23:35:46', '1', '304', '两人一间每人每天180', '', 2, 1),
    (82, 31, 29, '套餐图片', NULL, '2026-04-28 23:35:46', '1', '305', '/profile/upload/2026/04/28/room3_20260428233502A100.png', '', 1, 2),
    (83, 31, 29, '介绍', NULL, '2026-04-28 23:35:46', '1', '304', '一人一间每人每天280', '', 2, 2),
    (84, 31, 30, '天数', NULL, '2026-04-28 23:43:06', '1', '303', '8', '天', 1, 0),
    (85, 31, 30, '介绍', NULL, '2026-04-28 23:43:06', '1', '304', '两人一间每人每天180', '', 2, 1),
    (86, 31, 30, '总价', NULL, '2026-04-28 23:43:06', '1', '302', '1080', '元', 3, 1),
    (87, 31, 30, '均价', NULL, '2026-04-28 23:43:06', '1', '301', '180', '元', 4, 1),
    (88, 31, 30, '床位', NULL, '2026-04-28 23:43:06', '1', '303', '2', '床/间', 5, 1),
    (89, 31, 31, '天数', NULL, '2026-04-28 23:43:06', '1', '303', '31', '天', 1, 0),
    (90, 31, 31, '介绍', NULL, '2026-04-28 23:43:06', '1', '304', '两人一间每人每天143', '', 2, 2),
    (91, 31, 31, '总价', NULL, '2026-04-28 23:43:06', '1', '302', '4280', '元', 3, 2),
    (92, 31, 31, '均价', NULL, '2026-04-28 23:43:06', '1', '301', '143', '元', 4, 2),
    (93, 31, 31, '床位', NULL, '2026-04-28 23:43:06', '1', '303', '2', '床', 5, 2),
    (94, 31, 32, '天数', NULL, '2026-04-28 23:51:37', '1', '303', '8', '天', 1, 0),
    (95, 31, 32, '介绍', NULL, '2026-04-28 23:51:37', '1', '304', '一人一间每人每天280', '', 2, 21),
    (96, 31, 32, '总价', NULL, '2026-04-28 23:51:37', '1', '302', '1680', '元', 3, 21),
    (97, 31, 32, '均价', NULL, '2026-04-28 23:51:37', '1', '301', '280', '元', 4, 21),
    (98, 31, 32, '床位', NULL, '2026-04-28 23:51:37', '1', '303', '1', '床', 5, 21),
    (99, 31, 33, '天数', NULL, '2026-04-28 23:51:37', '1', '303', '31', '天', 1, 0),
    (100, 31, 33, '介绍', NULL, '2026-04-28 23:51:37', '1', '304', '一人一间每人每天216', '', 2, 22),
    (101, 31, 33, '总价', NULL, '2026-04-28 23:51:37', '1', '302', '6480', '元', 3, 22),
    (102, 31, 33, '均价', NULL, '2026-04-28 23:51:37', '1', '301', '216', '元', 4, 22),
    (103, 31, 33, '床位', NULL, '2026-04-28 23:51:37', '1', '303', '1', '床', 5, 22);

-- app_goods_sku_data: production returned no rows.

ALTER TABLE app_goods_category AUTO_INCREMENT = 62;
ALTER TABLE app_goods AUTO_INCREMENT = 39;
ALTER TABLE app_goods_related AUTO_INCREMENT = 326;
ALTER TABLE app_goods_education_ext AUTO_INCREMENT = 2;
ALTER TABLE app_goods_sku AUTO_INCREMENT = 36;
ALTER TABLE app_goods_sku_option AUTO_INCREMENT = 109;
ALTER TABLE app_goods_sku_data AUTO_INCREMENT = 1;
