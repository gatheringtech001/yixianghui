/** 客户资料表格列配置（多维表格 / 列分组） */
export const COLUMN_GROUPS = [
  {
    key: 'basic',
    label: '基础档案',
    props: ['consultantName', 'consultantId', 'customerName', 'deptName', 'customerNo', 'linkMobile', 'signTime', 'acquisitionChannel', 'customerLabel', 'customerInfo', 'idcard', 'sex', 'birthday', 'age']
  },
  {
    key: 'follow',
    label: '回访跟进',
    props: ['returnVisit', 'returnVisitRemark', 'returnVisitFirst', 'returnVisitSecond', 'returnVisitLast', 'insuranceEvaStatus']
  },
  {
    key: 'profile',
    label: '个人信息',
    props: ['nation', 'haveSpecialCard', 'education', 'religion', 'marital', 'liveAddress', 'idcardAddress', 'liveInro', 'houseType', 'familyGt65Count', 'familyDwalkCount']
  },
  {
    key: 'contact',
    label: '联络人',
    props: ['link1Name', 'link1Relation', 'link1Mobile', 'link2Name', 'link2Relation', 'link2Mobile', 'infoPersonName', 'infoPersonRelation']
  },
  {
    key: 'health',
    label: '健康评估',
    props: ['healthTest', 'diseaseDementia', 'diseaseMental', 'diseaseOther', 'medicationLong', 'checkUpYear', 'chronicDiseaseCheck', 'selfAbility']
  },
  {
    key: 'family',
    label: '家庭照护',
    props: ['membersEvaluate', 'children', 'childrenNearly', 'membersRelation', 'caregiver', 'caregiverCount', 'caregiverExperience', 'careContent', 'careTimes']
  },
  {
    key: 'policy',
    label: '养老政策',
    props: ['elderlyCareEvaluate', 'elderlyCareSubsidy', 'insuranceLongCare', 'medicalPayMethod', 'pensionMonth', 'elderlyCareReport']
  },
  {
    key: 'service',
    label: '服务需求',
    props: ['organizationCare', 'elderlyService', 'rehabilitationService', 'medicalInstitution', 'familyService', 'elderlyCareAi', 'chineseMedicalService', 'careToDoor', 'purchasedCemetery']
  },
  {
    key: 'other',
    label: '其他',
    props: ['consultant', 'remark']
  }
]

export const DEFAULT_VISIBLE_PROPS = [
  'consultantName',
  'customerName',
  'deptName',
  'customerNo',
  'linkMobile',
  'signTime',
  'customerLabel',
  'returnVisit',
  'acquisitionChannel',
  'sex',
  'age',
  'liveAddress'
]

const STORAGE_KEY = 'app_customer_visible_columns'

export const ALL_COLUMNS = [
  { prop: 'consultantName', label: '康养顾问', group: 'basic', minWidth: 100 },
  { prop: 'consultantId', label: '顾问ID', group: 'basic', width: 80 },
  { prop: 'customerName', label: '客户姓名', group: 'basic', minWidth: 100, fixed: 'left', sortable: true },
  { prop: 'deptName', label: '所属站点', group: 'basic', minWidth: 120 },
  { prop: 'customerNo', label: '客户编号', group: 'basic', width: 90, sortable: true },
  { prop: 'linkMobile', label: '联系电话', group: 'basic', width: 120, sortable: true },
  { prop: 'signTime', label: '登记日期', group: 'basic', width: 110, type: 'date', sortable: true },
  { prop: 'acquisitionChannel', label: '获客渠道', group: 'basic', width: 110, dict: 'hkqd' },
  { prop: 'customerLabel', label: '客户标签', group: 'basic', minWidth: 110, dict: 'khbq' },
  { prop: 'customerInfo', label: '信息录入', group: 'basic', width: 90 },
  { prop: 'idcard', label: '身份证号', group: 'basic', minWidth: 160 },
  { prop: 'sex', label: '性别', group: 'basic', width: 70, dict: 'sys_user_sex' },
  { prop: 'birthday', label: '出生日期', group: 'basic', width: 110, type: 'date', sortable: true },
  { prop: 'age', label: '岁数', group: 'basic', width: 70, sortable: true },
  { prop: 'returnVisit', label: '是否回访', group: 'follow', width: 90, dict: 'common_is_not' },
  { prop: 'returnVisitRemark', label: '回访记录', group: 'follow', minWidth: 120 },
  { prop: 'returnVisitFirst', label: '第一次回访', group: 'follow', width: 110, type: 'date', sortable: true },
  { prop: 'returnVisitSecond', label: '第二次回访', group: 'follow', width: 110, type: 'date', sortable: true },
  { prop: 'returnVisitLast', label: '最近回访', group: 'follow', width: 110, type: 'date', sortable: true },
  { prop: 'insuranceEvaStatus', label: '长护险评估', group: 'follow', minWidth: 110 },
  { prop: 'nation', label: '民族', group: 'profile', width: 90, dict: 'nation' },
  { prop: 'haveSpecialCard', label: '特病卡', group: 'profile', width: 90, dict: 'common_is_not' },
  { prop: 'education', label: '文化程度', group: 'profile', minWidth: 100, dict: 'whcd' },
  { prop: 'religion', label: '宗教信仰', group: 'profile', width: 90, dict: 'zjxy' },
  { prop: 'marital', label: '婚姻状况', group: 'profile', width: 90, dict: 'hyzk' },
  { prop: 'liveAddress', label: '现居住地址', group: 'profile', minWidth: 140 },
  { prop: 'idcardAddress', label: '户口所在地', group: 'profile', minWidth: 140 },
  { prop: 'liveInro', label: '居住情况', group: 'profile', minWidth: 110 },
  { prop: 'houseType', label: '住宅类型', group: 'profile', width: 90, dict: 'common_is_not' },
  { prop: 'familyGt65Count', label: '65岁及以上人数', group: 'profile', width: 120 },
  { prop: 'familyDwalkCount', label: '行动不便人数', group: 'profile', width: 120 },
  { prop: 'link1Name', label: '联络人1姓名', group: 'contact', width: 100 },
  { prop: 'link1Relation', label: '联络人1关系', group: 'contact', width: 100, dict: 'ykhgx' },
  { prop: 'link1Mobile', label: '联络人1电话', group: 'contact', width: 120 },
  { prop: 'link2Name', label: '联络人2姓名', group: 'contact', width: 100 },
  { prop: 'link2Relation', label: '联络人2关系', group: 'contact', width: 100, dict: 'ykhgx' },
  { prop: 'link2Mobile', label: '联络人2电话', group: 'contact', width: 120 },
  { prop: 'infoPersonName', label: '信息提供者', group: 'contact', width: 100 },
  { prop: 'infoPersonRelation', label: '提供者关系', group: 'contact', width: 100, dict: 'ykhgx' },
  { prop: 'healthTest', label: '身体状况评估', group: 'health', width: 110, dict: 'common_is_not' },
  { prop: 'diseaseDementia', label: '认知症', group: 'health', width: 90, dict: 'jbzdrzz' },
  { prop: 'diseaseMental', label: '精神疾病', group: 'health', width: 90, dict: 'jbzdjsjb' },
  { prop: 'diseaseOther', label: '其他疾病', group: 'health', minWidth: 100 },
  { prop: 'medicationLong', label: '长期服药', group: 'health', width: 100, dict: 'common_is_not2' },
  { prop: 'checkUpYear', label: '每年体检', group: 'health', width: 90, dict: 'common_is_not2' },
  { prop: 'chronicDiseaseCheck', label: '慢性病就诊', group: 'health', width: 100, dict: 'common_is_not' },
  { prop: 'selfAbility', label: '自理能力', group: 'health', width: 90, dict: 'zlnl' },
  { prop: 'membersEvaluate', label: '成员评估', group: 'family', width: 90, dict: 'common_is_not' },
  { prop: 'children', label: '子女情况', group: 'family', minWidth: 100 },
  { prop: 'childrenNearly', label: '子女当地工作', group: 'family', width: 110, dict: 'common_is_not' },
  { prop: 'membersRelation', label: '家庭情感关系', group: 'family', minWidth: 110 },
  { prop: 'caregiver', label: '有无照护者', group: 'family', width: 100, dict: 'common_is_not' },
  { prop: 'caregiverCount', label: '照护者数量', group: 'family', width: 100 },
  { prop: 'caregiverExperience', label: '照护经验', group: 'family', width: 100, dict: 'zhjy' },
  { prop: 'careContent', label: '照护内容', group: 'family', width: 100, dict: 'zhnr' },
  { prop: 'careTimes', label: '照护时间', group: 'family', width: 100, dict: 'zhsj' },
  { prop: 'elderlyCareEvaluate', label: '养老政策评估', group: 'policy', width: 110, dict: 'common_is_not' },
  { prop: 'elderlyCareSubsidy', label: '养老补贴', group: 'policy', width: 100, dict: 'ylfwbt' },
  { prop: 'insuranceLongCare', label: '长护险', group: 'policy', width: 90, dict: 'common_is_not' },
  { prop: 'medicalPayMethod', label: '医疗支付', group: 'policy', width: 100, dict: 'ylzhfs' },
  { prop: 'pensionMonth', label: '月退休金', group: 'policy', width: 100 },
  { prop: 'elderlyCareReport', label: '需求评估报告', group: 'policy', width: 110, dict: 'common_is_not' },
  { prop: 'organizationCare', label: '机构托养', group: 'service', minWidth: 100 },
  { prop: 'elderlyService', label: '为老服务中心', group: 'service', minWidth: 110 },
  { prop: 'rehabilitationService', label: '康复服务', group: 'service', minWidth: 100 },
  { prop: 'medicalInstitution', label: '医疗机构', group: 'service', minWidth: 100 },
  { prop: 'familyService', label: '居家服务', group: 'service', minWidth: 100 },
  { prop: 'elderlyCareAi', label: '适老化智能', group: 'service', minWidth: 100 },
  { prop: 'chineseMedicalService', label: '中医服务', group: 'service', minWidth: 100 },
  { prop: 'careToDoor', label: '上门护理', group: 'service', minWidth: 100 },
  { prop: 'purchasedCemetery', label: '购买福地', group: 'service', width: 90, dict: 'common_is_not2' },
  { prop: 'consultant', label: '外部顾问', group: 'other', minWidth: 100 },
  { prop: 'remark', label: '备注', group: 'other', minWidth: 120 }
]

export function loadVisibleColumnProps() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      if (Array.isArray(parsed) && parsed.length) return parsed
    }
  } catch (e) {}
  return [...DEFAULT_VISIBLE_PROPS]
}

export function saveVisibleColumnProps(props) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(props))
}

export function getColumnsByProps(props) {
  const map = ALL_COLUMNS.reduce((acc, col) => {
    acc[col.prop] = col
    return acc
  }, {})
  return props.map(prop => map[prop]).filter(Boolean)
}
