package com.ruoyi.system.domain;

import java.time.LocalDate;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户资料对象 app_customer
 * 
 * @author lankong
 * @date 2025-05-07
 */
public class AppCustomer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 客户ID */
    private Long customerId;

    /** 系统用户ID */
    @Excel(name = "系统用户ID")
    private Long userId;

    /** 客户姓名 */
    @Excel(name = "客户姓名")
    private String customerName;

    /** 消费记录 */
    @Excel(name = "消费记录")
    private String buyRecords;

    /** 所属站点 */
//    @Excel(name = "所属站点")
    private Long deptId;

    /** 客户编号 */
    @Excel(name = "客户编号")
    private String customerNo;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String linkMobile;

    /** 登记日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "登记日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date signTime;

    /** 长护险评估状态 */
    @Excel(name = "长护险评估状态")
    private String insuranceEvaStatus;

    /** 是否进行回访 */
    @Excel(name = "是否进行回访")
    private Integer returnVisit;

    /** 回访记录 */
    @Excel(name = "回访记录")
    private String returnVisitRemark;

    /** 第一次回访时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "第一次回访时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date returnVisitFirst;

    /** 第二次回访时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "第二次回访时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date returnVisitSecond;

    /** 最近一次回访时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "最近一次回访时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date returnVisitLast;

    /** 获客渠道 */
    @Excel(name = "获客渠道")
    private String acquisitionChannel;

    /** 客户标签 */
    @Excel(name = "客户标签")
    private String customerLabel;

    /** 是否进行客户信息录入 */
    @Excel(name = "是否进行客户信息录入")
    private String customerInfo;

    /** 客户需求产品 */
    @Excel(name = "客户需求产品")
    private String customerGoods;

    /** 养老顾问（组织内） */
//    @Excel(name = "养老顾问")
    private Long consultantId;

    /** 身份证号 */
    @Excel(name = "身份证号")
    private String idcard;

    /** 性别 */
    @Excel(name = "性别")
    private String sex;

    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDate birthday;

    /** 岁数 */
    @Excel(name = "岁数")
    private Integer age;

    /** 民族 */
    @Excel(name = "民族")
    private String nation;

    /** 是否持有特病卡 */
    @Excel(name = "是否持有特病卡")
    private String haveSpecialCard;

    /** 文化程度 */
    @Excel(name = "文化程度")
    private String education;

    /** 宗教信仰 */
    @Excel(name = "宗教信仰")
    private String religion;

    /** 婚姻状况 */
    @Excel(name = "婚姻状况")
    private String marital;

    /** 现居住地址 */
    @Excel(name = "现居住地址")
    private String liveAddress;

    /** 户口所在地 */
    @Excel(name = "户口所在地")
    private String idcardAddress;

    /** 居住情况 */
    @Excel(name = "居住情况")
    private String liveInro;

    /** 住宅类型 */
    @Excel(name = "住宅类型")
    private String houseType;

    /** 家庭中有65岁及以上的人数 */
    @Excel(name = "家庭中有65岁及以上的人数")
    private Integer familyGt65Count;

    /** 家中是行动不便的人数 */
    @Excel(name = "家中是行动不便的人数")
    private Integer familyDwalkCount;

    /** 联络人（1）姓名 */
    @Excel(name = "联络人")
    private String link1Name;

    /** 联络人（1）与老人关系 */
    @Excel(name = "联络人")
    private String link1Relation;

    /** 联络人（1）联系方式 */
    @Excel(name = "联络人")
    private String link1Mobile;

    /** 联络人（2）姓名 */
    @Excel(name = "联络人")
    private String link2Name;

    /** 联络人（2）与老人关系 */
    @Excel(name = "联络人")
    private String link2Relation;

    /** 联络人（2）联系方式 */
    @Excel(name = "联络人")
    private String link2Mobile;

    /** 信息提供者姓名 */
    @Excel(name = "信息提供者姓名")
    private String infoPersonName;

    /** 信息提供者与老人关系 */
    @Excel(name = "信息提供者与老人关系")
    private String infoPersonRelation;

    /** 一、是否进行身体状况评估 */
    @Excel(name = "一、是否进行身体状况评估")
    private String healthTest;

    /** 疾病诊断-&gt;痴呆 */
    @Excel(name = "疾病诊断-&gt;痴呆")
    private String diseaseDementia;

    /** 疾病诊断-&gt;精神疾病 */
    @Excel(name = "疾病诊断-&gt;精神疾病")
    private String diseaseMental;

    /** 疾病诊断-&gt;躯体疾病 */
    @Excel(name = "疾病诊断-&gt;躯体疾病")
    private String diseaseBody;

    /** 疾病诊断-&gt;其他疾病 */
    @Excel(name = "疾病诊断-&gt;其他疾病")
    private String diseaseOther;

    /** 有无长期服药|中医|理疗|中医茶饮的情况 */
    @Excel(name = "有无长期服药|中医|理疗|中医茶饮的情况")
    private String medicationLong;

    /** 是否每年体检 */
    @Excel(name = "是否每年体检")
    private String checkUpYear;

    /** 慢性病是否定期就诊 */
    @Excel(name = "慢性病是否定期就诊")
    private String chronicDiseaseCheck;

    /** 自理能力 */
    @Excel(name = "自理能力")
    private String selfAbility;

    /** 二、是否进行成员评估 */
    @Excel(name = "二、是否进行成员评估")
    private String membersEvaluate;

    /** 子女情况 */
    @Excel(name = "子女情况")
    private String children;

    /** 子女是否在当地工作 */
    @Excel(name = "子女是否在当地工作")
    private String childrenNearly;

    /** 与家庭成员的情感关系 */
    @Excel(name = "与家庭成员的情感关系")
    private String membersRelation;

    /** 有无照护者 */
    @Excel(name = "有无照护者")
    private String caregiver;

    /** 照护者数量 */
    @Excel(name = "照护者数量")
    private Integer caregiverCount;

    /** 照护者是否有照护经验 */
    @Excel(name = "照护者是否有照护经验")
    private String caregiverExperience;

    /** 照护内容 */
    @Excel(name = "照护内容")
    private String careContent;

    /** 照护时间 */
    @Excel(name = "照护时间")
    private String careTimes;

    /** 三、是否进行养老政策评估 */
    @Excel(name = "三、是否进行养老政策评估")
    private String elderlyCareEvaluate;

    /** 享有养老（助残）服务补贴 */
    @Excel(name = "享有养老（助残）服务补贴")
    private String elderlyCareSubsidy;

    /** 是否享受长期护理保险 */
    @Excel(name = "是否享受长期护理保险")
    private String insuranceLongCare;

    /** 医疗支付方式 */
    @Excel(name = "医疗支付方式")
    private String medicalPayMethod;

    /** 医疗支付方式（备注） */
    @Excel(name = "医疗支付方式")
    private String medicalPayMethodRemark;

    /** 月退休金|养老金 */
    @Excel(name = "月退休金|养老金")
    private String pensionMonth;

    /** 是否进行长者养老需求评估报告 */
    @Excel(name = "是否进行长者养老需求评估报告")
    private String elderlyCareReport;

    /** 机构托养 */
    @Excel(name = "机构托养")
    private String organizationCare;

    /** 综合为老服务中心 */
    @Excel(name = "综合为老服务中心")
    private String elderlyService;

    /** 康复服务 */
    @Excel(name = "康复服务")
    private String rehabilitationService;

    /** 医疗机构 */
    @Excel(name = "医疗机构")
    private String medicalInstitution;

    /** 居家服务 */
    @Excel(name = "居家服务")
    private String familyService;

    /** 适老化智能化养老 */
    @Excel(name = "适老化智能化养老")
    private String elderlyCareAi;

    /** 中医服务 */
    @Excel(name = "中医服务")
    private String chineseMedicalService;

    /** 非医疗性护理（上门服务） */
    @Excel(name = "非医疗性护理")
    private String careToDoor;

    /** 是否购买福地 */
    @Excel(name = "是否购买福地")
    private String purchasedCemetery;

    /** 附件 */
    @Excel(name = "附件")
    private String attach;

    /** 养老顾问 */
    @Excel(name = "养老顾问")
    private String consultant;

    /** 医养入住情况 */
    @Excel(name = "医养入住情况")
    private String medicalCare;

    /** 父记录 */
    @Excel(name = "父记录")
    private String parentRecord;

    /** 客户状态 */
    @Excel(name = "客户状态")
    private String status;

    /** 服用药品名称及使用方式及剂量 */
    @Excel(name = "服用药品名称及使用方式及剂量")
    private String medicationRemark;

    /** 子女或其他补贴 */
    @Excel(name = "子女或其他补贴")
    private String childPensionMonth;

    /** 删除状态 */
    private String delFlag;

    //创建人
    @Excel(name = "创建人")
    private transient String creatorName;
    //站点名称
    @Excel(name = "所属站点")
    private transient String deptName;
    //康养顾问
    @Excel(name = "康养顾问")
    private transient String consultantName;

    public void setCustomerId(Long customerId) 
    {
        this.customerId = customerId;
    }

    public Long getCustomerId() 
    {
        return customerId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setCustomerName(String customerName) 
    {
        this.customerName = customerName;
    }

    public String getCustomerName() 
    {
        return customerName;
    }

    public void setBuyRecords(String buyRecords) 
    {
        this.buyRecords = buyRecords;
    }

    public String getBuyRecords() 
    {
        return buyRecords;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setCustomerNo(String customerNo) 
    {
        this.customerNo = customerNo;
    }

    public String getCustomerNo() 
    {
        return customerNo;
    }

    public void setLinkMobile(String linkMobile) 
    {
        this.linkMobile = linkMobile;
    }

    public String getLinkMobile() 
    {
        return linkMobile;
    }

    public void setSignTime(Date signTime) 
    {
        this.signTime = signTime;
    }

    public Date getSignTime() 
    {
        return signTime;
    }

    public void setInsuranceEvaStatus(String insuranceEvaStatus) 
    {
        this.insuranceEvaStatus = insuranceEvaStatus;
    }

    public String getInsuranceEvaStatus() 
    {
        return insuranceEvaStatus;
    }

    public void setReturnVisit(Integer returnVisit) 
    {
        this.returnVisit = returnVisit;
    }

    public Integer getReturnVisit() 
    {
        return returnVisit;
    }

    public void setReturnVisitRemark(String returnVisitRemark) 
    {
        this.returnVisitRemark = returnVisitRemark;
    }

    public String getReturnVisitRemark() 
    {
        return returnVisitRemark;
    }

    public void setReturnVisitFirst(Date returnVisitFirst) 
    {
        this.returnVisitFirst = returnVisitFirst;
    }

    public Date getReturnVisitFirst() 
    {
        return returnVisitFirst;
    }

    public void setReturnVisitSecond(Date returnVisitSecond) 
    {
        this.returnVisitSecond = returnVisitSecond;
    }

    public Date getReturnVisitSecond() 
    {
        return returnVisitSecond;
    }

    public void setReturnVisitLast(Date returnVisitLast) 
    {
        this.returnVisitLast = returnVisitLast;
    }

    public Date getReturnVisitLast() 
    {
        return returnVisitLast;
    }

    public void setAcquisitionChannel(String acquisitionChannel) 
    {
        this.acquisitionChannel = acquisitionChannel;
    }

    public String getAcquisitionChannel() 
    {
        return acquisitionChannel;
    }

    public void setCustomerLabel(String customerLabel) 
    {
        this.customerLabel = customerLabel;
    }

    public String getCustomerLabel() 
    {
        return customerLabel;
    }

    public void setCustomerInfo(String customerInfo) 
    {
        this.customerInfo = customerInfo;
    }

    public String getCustomerInfo() 
    {
        return customerInfo;
    }

    public void setCustomerGoods(String customerGoods) 
    {
        this.customerGoods = customerGoods;
    }

    public String getCustomerGoods() 
    {
        return customerGoods;
    }

    public void setConsultantId(Long consultantId) 
    {
        this.consultantId = consultantId;
    }

    public Long getConsultantId() 
    {
        return consultantId;
    }

    public void setIdcard(String idcard) 
    {
        this.idcard = idcard;
    }

    public String getIdcard() 
    {
        return idcard;
    }

    public void setSex(String sex) 
    {
        this.sex = sex;
    }

    public String getSex() 
    {
        return sex;
    }

    public void setBirthday(LocalDate birthday)
    {
        this.birthday = birthday;
    }

    public LocalDate getBirthday()
    {
        return birthday;
    }

    public void setAge(Integer age) 
    {
        this.age = age;
    }

    public Integer getAge() 
    {
        return age;
    }

    public void setNation(String nation) 
    {
        this.nation = nation;
    }

    public String getNation() 
    {
        return nation;
    }

    public void setHaveSpecialCard(String haveSpecialCard) 
    {
        this.haveSpecialCard = haveSpecialCard;
    }

    public String getHaveSpecialCard() 
    {
        return haveSpecialCard;
    }

    public void setEducation(String education) 
    {
        this.education = education;
    }

    public String getEducation() 
    {
        return education;
    }

    public void setReligion(String religion) 
    {
        this.religion = religion;
    }

    public String getReligion() 
    {
        return religion;
    }

    public void setMarital(String marital) 
    {
        this.marital = marital;
    }

    public String getMarital() 
    {
        return marital;
    }

    public void setLiveAddress(String liveAddress) 
    {
        this.liveAddress = liveAddress;
    }

    public String getLiveAddress() 
    {
        return liveAddress;
    }

    public void setIdcardAddress(String idcardAddress) 
    {
        this.idcardAddress = idcardAddress;
    }

    public String getIdcardAddress() 
    {
        return idcardAddress;
    }

    public void setLiveInro(String liveInro) 
    {
        this.liveInro = liveInro;
    }

    public String getLiveInro() 
    {
        return liveInro;
    }

    public void setHouseType(String houseType) 
    {
        this.houseType = houseType;
    }

    public String getHouseType() 
    {
        return houseType;
    }

    public void setFamilyGt65Count(Integer familyGt65Count) 
    {
        this.familyGt65Count = familyGt65Count;
    }

    public Integer getFamilyGt65Count() 
    {
        return familyGt65Count;
    }

    public void setFamilyDwalkCount(Integer familyDwalkCount) 
    {
        this.familyDwalkCount = familyDwalkCount;
    }

    public Integer getFamilyDwalkCount() 
    {
        return familyDwalkCount;
    }

    public void setLink1Name(String link1Name) 
    {
        this.link1Name = link1Name;
    }

    public String getLink1Name() 
    {
        return link1Name;
    }

    public void setLink1Relation(String link1Relation) 
    {
        this.link1Relation = link1Relation;
    }

    public String getLink1Relation() 
    {
        return link1Relation;
    }

    public void setLink1Mobile(String link1Mobile) 
    {
        this.link1Mobile = link1Mobile;
    }

    public String getLink1Mobile() 
    {
        return link1Mobile;
    }

    public void setLink2Name(String link2Name) 
    {
        this.link2Name = link2Name;
    }

    public String getLink2Name() 
    {
        return link2Name;
    }

    public void setLink2Relation(String link2Relation) 
    {
        this.link2Relation = link2Relation;
    }

    public String getLink2Relation() 
    {
        return link2Relation;
    }

    public void setLink2Mobile(String link2Mobile) 
    {
        this.link2Mobile = link2Mobile;
    }

    public String getLink2Mobile() 
    {
        return link2Mobile;
    }

    public void setInfoPersonName(String infoPersonName) 
    {
        this.infoPersonName = infoPersonName;
    }

    public String getInfoPersonName() 
    {
        return infoPersonName;
    }

    public void setInfoPersonRelation(String infoPersonRelation) 
    {
        this.infoPersonRelation = infoPersonRelation;
    }

    public String getInfoPersonRelation() 
    {
        return infoPersonRelation;
    }

    public void setHealthTest(String healthTest) 
    {
        this.healthTest = healthTest;
    }

    public String getHealthTest() 
    {
        return healthTest;
    }

    public void setDiseaseDementia(String diseaseDementia) 
    {
        this.diseaseDementia = diseaseDementia;
    }

    public String getDiseaseDementia() 
    {
        return diseaseDementia;
    }

    public void setDiseaseMental(String diseaseMental) 
    {
        this.diseaseMental = diseaseMental;
    }

    public String getDiseaseMental() 
    {
        return diseaseMental;
    }

    public String getDiseaseBody() {
        return diseaseBody;
    }

    public void setDiseaseBody(String diseaseBody) {
        this.diseaseBody = diseaseBody;
    }

    public void setDiseaseOther(String diseaseOther)
    {
        this.diseaseOther = diseaseOther;
    }

    public String getDiseaseOther() 
    {
        return diseaseOther;
    }

    public void setMedicationLong(String medicationLong) 
    {
        this.medicationLong = medicationLong;
    }

    public String getMedicationLong() 
    {
        return medicationLong;
    }

    public void setCheckUpYear(String checkUpYear) 
    {
        this.checkUpYear = checkUpYear;
    }

    public String getCheckUpYear() 
    {
        return checkUpYear;
    }

    public void setChronicDiseaseCheck(String chronicDiseaseCheck) 
    {
        this.chronicDiseaseCheck = chronicDiseaseCheck;
    }

    public String getChronicDiseaseCheck() 
    {
        return chronicDiseaseCheck;
    }

    public void setSelfAbility(String selfAbility) 
    {
        this.selfAbility = selfAbility;
    }

    public String getSelfAbility() 
    {
        return selfAbility;
    }

    public void setMembersEvaluate(String membersEvaluate) 
    {
        this.membersEvaluate = membersEvaluate;
    }

    public String getMembersEvaluate() 
    {
        return membersEvaluate;
    }

    public void setChildren(String children) 
    {
        this.children = children;
    }

    public String getChildren() 
    {
        return children;
    }

    public void setChildrenNearly(String childrenNearly) 
    {
        this.childrenNearly = childrenNearly;
    }

    public String getChildrenNearly() 
    {
        return childrenNearly;
    }

    public void setMembersRelation(String membersRelation) 
    {
        this.membersRelation = membersRelation;
    }

    public String getMembersRelation() 
    {
        return membersRelation;
    }

    public void setCaregiver(String caregiver) 
    {
        this.caregiver = caregiver;
    }

    public String getCaregiver() 
    {
        return caregiver;
    }

    public void setCaregiverCount(Integer caregiverCount) 
    {
        this.caregiverCount = caregiverCount;
    }

    public Integer getCaregiverCount() 
    {
        return caregiverCount;
    }

    public void setCaregiverExperience(String caregiverExperience) 
    {
        this.caregiverExperience = caregiverExperience;
    }

    public String getCaregiverExperience() 
    {
        return caregiverExperience;
    }

    public void setCareContent(String careContent) 
    {
        this.careContent = careContent;
    }

    public String getCareContent() 
    {
        return careContent;
    }

    public void setCareTimes(String careTimes) 
    {
        this.careTimes = careTimes;
    }

    public String getCareTimes() 
    {
        return careTimes;
    }

    public void setElderlyCareEvaluate(String elderlyCareEvaluate) 
    {
        this.elderlyCareEvaluate = elderlyCareEvaluate;
    }

    public String getElderlyCareEvaluate() 
    {
        return elderlyCareEvaluate;
    }

    public void setElderlyCareSubsidy(String elderlyCareSubsidy) 
    {
        this.elderlyCareSubsidy = elderlyCareSubsidy;
    }

    public String getElderlyCareSubsidy() 
    {
        return elderlyCareSubsidy;
    }

    public void setInsuranceLongCare(String insuranceLongCare) 
    {
        this.insuranceLongCare = insuranceLongCare;
    }

    public String getInsuranceLongCare() 
    {
        return insuranceLongCare;
    }

    public void setMedicalPayMethod(String medicalPayMethod) 
    {
        this.medicalPayMethod = medicalPayMethod;
    }

    public String getMedicalPayMethod() 
    {
        return medicalPayMethod;
    }

    public void setMedicalPayMethodRemark(String medicalPayMethodRemark) 
    {
        this.medicalPayMethodRemark = medicalPayMethodRemark;
    }

    public String getMedicalPayMethodRemark() 
    {
        return medicalPayMethodRemark;
    }

    public void setPensionMonth(String pensionMonth) 
    {
        this.pensionMonth = pensionMonth;
    }

    public String getPensionMonth() 
    {
        return pensionMonth;
    }

    public void setElderlyCareReport(String elderlyCareReport) 
    {
        this.elderlyCareReport = elderlyCareReport;
    }

    public String getElderlyCareReport() 
    {
        return elderlyCareReport;
    }

    public void setOrganizationCare(String organizationCare) 
    {
        this.organizationCare = organizationCare;
    }

    public String getOrganizationCare() 
    {
        return organizationCare;
    }

    public void setElderlyService(String elderlyService) 
    {
        this.elderlyService = elderlyService;
    }

    public String getElderlyService() 
    {
        return elderlyService;
    }

    public void setRehabilitationService(String rehabilitationService) 
    {
        this.rehabilitationService = rehabilitationService;
    }

    public String getRehabilitationService() 
    {
        return rehabilitationService;
    }

    public void setMedicalInstitution(String medicalInstitution) 
    {
        this.medicalInstitution = medicalInstitution;
    }

    public String getMedicalInstitution() 
    {
        return medicalInstitution;
    }

    public void setFamilyService(String familyService) 
    {
        this.familyService = familyService;
    }

    public String getFamilyService() 
    {
        return familyService;
    }

    public void setElderlyCareAi(String elderlyCareAi) 
    {
        this.elderlyCareAi = elderlyCareAi;
    }

    public String getElderlyCareAi() 
    {
        return elderlyCareAi;
    }

    public void setChineseMedicalService(String chineseMedicalService) 
    {
        this.chineseMedicalService = chineseMedicalService;
    }

    public String getChineseMedicalService() 
    {
        return chineseMedicalService;
    }

    public void setCareToDoor(String careToDoor) 
    {
        this.careToDoor = careToDoor;
    }

    public String getCareToDoor() 
    {
        return careToDoor;
    }

    public void setPurchasedCemetery(String purchasedCemetery) 
    {
        this.purchasedCemetery = purchasedCemetery;
    }

    public String getPurchasedCemetery() 
    {
        return purchasedCemetery;
    }

    public void setAttach(String attach) 
    {
        this.attach = attach;
    }

    public String getAttach() 
    {
        return attach;
    }

    public void setConsultant(String consultant) 
    {
        this.consultant = consultant;
    }

    public String getConsultant() 
    {
        return consultant;
    }

    public void setMedicalCare(String medicalCare) 
    {
        this.medicalCare = medicalCare;
    }

    public String getMedicalCare() 
    {
        return medicalCare;
    }

    public void setParentRecord(String parentRecord) 
    {
        this.parentRecord = parentRecord;
    }

    public String getParentRecord() 
    {
        return parentRecord;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }


    public String getMedicationRemark() {
        return medicationRemark;
    }

    public void setMedicationRemark(String medicationRemark) {
        this.medicationRemark = medicationRemark;
    }

    public String getChildPensionMonth() {
        return childPensionMonth;
    }

    public void setChildPensionMonth(String childPensionMonth) {
        this.childPensionMonth = childPensionMonth;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("customerId", getCustomerId())
            .append("userId", getUserId())
            .append("customerName", getCustomerName())
            .append("buyRecords", getBuyRecords())
            .append("deptId", getDeptId())
            .append("customerNo", getCustomerNo())
            .append("linkMobile", getLinkMobile())
            .append("signTime", getSignTime())
            .append("insuranceEvaStatus", getInsuranceEvaStatus())
            .append("returnVisit", getReturnVisit())
            .append("returnVisitRemark", getReturnVisitRemark())
            .append("returnVisitFirst", getReturnVisitFirst())
            .append("returnVisitSecond", getReturnVisitSecond())
            .append("returnVisitLast", getReturnVisitLast())
            .append("acquisitionChannel", getAcquisitionChannel())
            .append("customerLabel", getCustomerLabel())
            .append("customerInfo", getCustomerInfo())
            .append("customerGoods", getCustomerGoods())
            .append("consultantId", getConsultantId())
            .append("idcard", getIdcard())
            .append("sex", getSex())
            .append("birthday", getBirthday())
            .append("age", getAge())
            .append("nation", getNation())
            .append("haveSpecialCard", getHaveSpecialCard())
            .append("education", getEducation())
            .append("religion", getReligion())
            .append("marital", getMarital())
            .append("liveAddress", getLiveAddress())
            .append("idcardAddress", getIdcardAddress())
            .append("liveInro", getLiveInro())
            .append("houseType", getHouseType())
            .append("familyGt65Count", getFamilyGt65Count())
            .append("familyDwalkCount", getFamilyDwalkCount())
            .append("link1Name", getLink1Name())
            .append("link1Relation", getLink1Relation())
            .append("link1Mobile", getLink1Mobile())
            .append("link2Name", getLink2Name())
            .append("link2Relation", getLink2Relation())
            .append("link2Mobile", getLink2Mobile())
            .append("infoPersonName", getInfoPersonName())
            .append("infoPersonRelation", getInfoPersonRelation())
            .append("healthTest", getHealthTest())
            .append("diseaseDementia", getDiseaseDementia())
            .append("diseaseMental", getDiseaseMental())
                .append("diseaseBody", getDiseaseBody())
            .append("diseaseOther", getDiseaseOther())
            .append("medicationLong", getMedicationLong())
            .append("checkUpYear", getCheckUpYear())
            .append("chronicDiseaseCheck", getChronicDiseaseCheck())
            .append("selfAbility", getSelfAbility())
            .append("membersEvaluate", getMembersEvaluate())
            .append("children", getChildren())
            .append("childrenNearly", getChildrenNearly())
            .append("membersRelation", getMembersRelation())
            .append("caregiver", getCaregiver())
            .append("caregiverCount", getCaregiverCount())
            .append("caregiverExperience", getCaregiverExperience())
            .append("careContent", getCareContent())
            .append("careTimes", getCareTimes())
            .append("elderlyCareEvaluate", getElderlyCareEvaluate())
            .append("elderlyCareSubsidy", getElderlyCareSubsidy())
            .append("insuranceLongCare", getInsuranceLongCare())
            .append("medicalPayMethod", getMedicalPayMethod())
            .append("medicalPayMethodRemark", getMedicalPayMethodRemark())
            .append("pensionMonth", getPensionMonth())
            .append("elderlyCareReport", getElderlyCareReport())
            .append("organizationCare", getOrganizationCare())
            .append("elderlyService", getElderlyService())
            .append("rehabilitationService", getRehabilitationService())
            .append("medicalInstitution", getMedicalInstitution())
            .append("familyService", getFamilyService())
            .append("elderlyCareAi", getElderlyCareAi())
            .append("chineseMedicalService", getChineseMedicalService())
            .append("careToDoor", getCareToDoor())
            .append("purchasedCemetery", getPurchasedCemetery())
            .append("attach", getAttach())
            .append("consultant", getConsultant())
            .append("medicalCare", getMedicalCare())
            .append("parentRecord", getParentRecord())
            .append("remark", getRemark())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
                .append("medicationRemark", getMedicationRemark())
                .append("childPensionMonth", getChildPensionMonth())
            .toString();
    }
}
