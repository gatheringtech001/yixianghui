"""Deterministic target model for the two Feishu business bases."""

TARGET_TABLES = {
    ("eldercare", "客户档案"): "app_eldercare_customer_profile",
    ("eldercare", "🧾收入明细数据"): "app_customer_income_feishu",
    ("eldercare", "活动计划表"): "app_activity_plan_feishu",
    ("eldercare", "长护险客户跟进表"): "app_long_care_followup",
    ("eldercare", "医养机构客户跟进表"): "app_medical_care_followup",
    ("eldercare", "📊月度收入统计"): "app_eldercare_monthly_income",
    ("eldercare", "养老顾问列表"): "app_consultant_feishu",
    ("eldercare", "满意度调研"): "app_satisfaction_survey",
    ("travel", "客户信息表"): "app_travel_customer_profile",
    ("travel", "预订订单表"): "app_travel_order_profile",
    ("travel", "结算表"): "app_travel_settlement",
    ("travel", "基地表"): "app_travel_base",
    ("travel", "主播排期表"): "app_travel_host_schedule",
    ("travel", "主播列表"): "app_travel_host",
    ("travel", "新媒体账号列表"): "app_travel_media_account",
}

RELATION_TYPES = {"SingleLink", "DuplexLink"}
USER_TYPES = {"User", "CreatedUser", "ModifiedUser"}
ATTACHMENT_TYPES = {"Attachment"}
NUMBER_TYPES = {"Number", "Currency", "AutoNumber"}
DATE_TYPES = {"DateTime", "CreatedTime", "ModifiedTime"}


def column_name(field_id):
    """Stable ASCII column name; source label is retained as its SQL comment."""
    safe = "".join(ch.lower() for ch in field_id if ch.isalnum() or ch == "_")
    return "fs_" + safe


def storage_kind(ui_type):
    if ui_type in RELATION_TYPES:
        return "relation"
    if ui_type in USER_TYPES:
        return "user"
    if ui_type in ATTACHMENT_TYPES:
        return "attachment"
    return "column"


def needs_child_storage(ui_type):
    """Whether a field also needs normalized rows besides its physical column."""
    return ui_type in RELATION_TYPES | USER_TYPES | ATTACHMENT_TYPES


def sql_type(ui_type, values):
    if ui_type == "Checkbox":
        return "tinyint(1)"
    if ui_type in DATE_TYPES:
        return "datetime"
    if ui_type in NUMBER_TYPES:
        return "decimal(20,4)"
    if ui_type == "Formula":
        concrete = [v for v in values if v is not None]
        if concrete and all(isinstance(v, (int, float)) and not isinstance(v, bool) for v in concrete):
            return "decimal(20,4)"
    return "text"
