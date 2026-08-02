package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 收入明细对象 app_customer_income
 * 
 * @author lankong
 * @date 2025-05-14
 */
public class AppCustomerIncome extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 收入ID */
    private Long incomeId;

    /** 创建用户ID */
    @Excel(name = "创建用户ID")
    private Long userId;

    /** 销售内容 */
    @Excel(name = "销售内容")
    private String productName;

    /** 编号 */
    @Excel(name = "编号")
    private String incomeNo;

    /** 服务站点 */
    @Excel(name = "服务站点")
    private Long deptId;

    /** 充值金额 */
    @Excel(name = "充值金额")
    private BigDecimal chargeAmount;

    /** 消费金额 */
    @Excel(name = "消费金额")
    private BigDecimal purchaseAmount;

    /** 余额 */
    @Excel(name = "余额")
    private BigDecimal balance;

    /** 金币 */
    @Excel(name = "金币")
    private Long score;

    /** 成交日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "成交日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date tradeDate;

    /** 是否结算 */
    @Excel(name = "是否结算")
    private Integer settlement;

    /** 公司收入 */
    @Excel(name = "公司收入")
    private BigDecimal companyIncome;

    /** 管家提成 */
    @Excel(name = "管家提成")
    private BigDecimal consultantIncome;

    /** 产品类别 */
    @Excel(name = "产品类别")
    private String productType;

    /** 供应商 */
    @Excel(name = "供应商")
    private Long supplierId;

    /** 养老管家 */
    @Excel(name = "养老管家")
    private Long consultantId;

    private String departName;

    private String consultantName;

    private String supplierName;

    private String userName;

    public void setIncomeId(Long incomeId) 
    {
        this.incomeId = incomeId;
    }

    public Long getIncomeId() 
    {
        return incomeId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setProductName(String productName) 
    {
        this.productName = productName;
    }

    public String getProductName() 
    {
        return productName;
    }

    public void setIncomeNo(String incomeNo) 
    {
        this.incomeNo = incomeNo;
    }

    public String getIncomeNo() 
    {
        return incomeNo;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setChargeAmount(BigDecimal chargeAmount) 
    {
        this.chargeAmount = chargeAmount;
    }

    public BigDecimal getChargeAmount() 
    {
        return chargeAmount;
    }

    public void setPurchaseAmount(BigDecimal purchaseAmount) 
    {
        this.purchaseAmount = purchaseAmount;
    }

    public BigDecimal getPurchaseAmount() 
    {
        return purchaseAmount;
    }

    public void setBalance(BigDecimal balance) 
    {
        this.balance = balance;
    }

    public BigDecimal getBalance() 
    {
        return balance;
    }

    public void setScore(Long score) 
    {
        this.score = score;
    }

    public Long getScore() 
    {
        return score;
    }

    public void setTradeDate(Date tradeDate) 
    {
        this.tradeDate = tradeDate;
    }

    public Date getTradeDate() 
    {
        return tradeDate;
    }

    public void setSettlement(Integer settlement) 
    {
        this.settlement = settlement;
    }

    public Integer getSettlement() 
    {
        return settlement;
    }

    public void setCompanyIncome(BigDecimal companyIncome) 
    {
        this.companyIncome = companyIncome;
    }

    public BigDecimal getCompanyIncome() 
    {
        return companyIncome;
    }

    public void setConsultantIncome(BigDecimal consultantIncome) 
    {
        this.consultantIncome = consultantIncome;
    }

    public BigDecimal getConsultantIncome() 
    {
        return consultantIncome;
    }

    public void setProductType(String productType) 
    {
        this.productType = productType;
    }

    public String getProductType() 
    {
        return productType;
    }

    public void setSupplierId(Long supplierId) 
    {
        this.supplierId = supplierId;
    }

    public Long getSupplierId() 
    {
        return supplierId;
    }

    public void setConsultantId(Long consultantId) 
    {
        this.consultantId = consultantId;
    }

    public Long getConsultantId() 
    {
        return consultantId;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("incomeId", getIncomeId())
            .append("userId", getUserId())
            .append("productName", getProductName())
            .append("incomeNo", getIncomeNo())
            .append("deptId", getDeptId())
            .append("chargeAmount", getChargeAmount())
            .append("purchaseAmount", getPurchaseAmount())
            .append("balance", getBalance())
            .append("score", getScore())
            .append("tradeDate", getTradeDate())
            .append("settlement", getSettlement())
            .append("companyIncome", getCompanyIncome())
            .append("consultantIncome", getConsultantIncome())
            .append("productType", getProductType())
            .append("supplierId", getSupplierId())
            .append("remark", getRemark())
            .append("consultantId", getConsultantId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
