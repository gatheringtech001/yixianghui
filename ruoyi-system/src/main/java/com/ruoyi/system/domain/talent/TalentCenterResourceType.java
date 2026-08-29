package com.ruoyi.system.domain.talent;

public enum TalentCenterResourceType
{
    GOODS("goods", "system:app_goods:list", "system:app_goods:query", "system:app_goods:edit"),
    ACTIVITY("activity", "system:app_activity:list", "system:app_activity:query", "system:app_activity:edit"),
    ARTICLE("article", "system:app_article:list", "system:app_article:query", "system:app_article:edit"),
    AD("ad", "system:app_ad_content:list", "system:app_ad_content:query", "system:app_ad_content:edit");

    private final String value;
    private final String listPermission;
    private final String queryPermission;
    private final String editPermission;

    TalentCenterResourceType(String value, String listPermission, String queryPermission, String editPermission)
    {
        this.value = value;
        this.listPermission = listPermission;
        this.queryPermission = queryPermission;
        this.editPermission = editPermission;
    }

    public static TalentCenterResourceType from(String value)
    {
        for (TalentCenterResourceType type : values())
        {
            if (type.value.equals(value))
            {
                return type;
            }
        }
        throw new TalentCenterApiException(400, "资源类型只允许 goods、activity、article、ad");
    }

    public String getValue() { return value; }
    public String getListPermission() { return listPermission; }
    public String getQueryPermission() { return queryPermission; }
    public String getEditPermission() { return editPermission; }
}
