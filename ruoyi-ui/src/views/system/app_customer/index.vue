<template>
  <div class="app-container customer-page">
    <div class="filter-card" v-show="showSearch">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px" class="customer-search-form">
      <div class="search-primary">
        <el-form-item label="快捷搜索" class="keyword-item">
          <el-input
            v-model="searchKeyword"
            placeholder="姓名 / 编号 / 电话 / 身份证"
            clearable
            style="width: 280px"
            @keyup.enter.native="handleQuery"
            @input="debouncedQuery"
            @clear="handleQuery"
          >
            <el-button slot="append" icon="el-icon-search" @click="handleQuery"></el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="康养顾问" prop="consultantId">
          <el-select v-model="queryParams.consultantId" filterable clearable placeholder="请选择康养顾问" style="width: 220px" @change="handleQuery">
            <el-option
              v-for="item in consultantOptions"
              :key="item.consultantId"
              :label="consultantOptionLabel(item)"
              :value="item.consultantId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="登记日期">
          <el-date-picker
            v-model="signDateRange"
            style="width: 240px"
            value-format="yyyy-MM-dd"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          <el-button type="text" size="mini" @click="showAdvancedSearch = !showAdvancedSearch">
            {{ showAdvancedSearch ? '收起筛选' : '更多筛选' }}
            <i :class="showAdvancedSearch ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
          </el-button>
        </el-form-item>
      </div>
      <div v-show="showAdvancedSearch" class="search-advanced">
        <el-form-item label="客户姓名" prop="customerName">
          <el-input
            v-model="queryParams.customerName"
            placeholder="请输入客户姓名"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="客户编号" prop="customerNo">
          <el-input
            v-model="queryParams.customerNo"
            placeholder="请输入客户编号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="身份证号" prop="idcard">
          <el-input
            v-model="queryParams.idcard"
            placeholder="请输入身份证号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="linkMobile">
          <el-input
            v-model="queryParams.linkMobile"
            placeholder="请输入联系电话"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="客户标签" prop="customerLabel">
          <el-select v-model="queryParams.customerLabel" clearable placeholder="请选择客户标签" style="width: 160px" @change="handleQuery">
            <el-option
              v-for="dict in dict.type.khbq"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="获客渠道" prop="acquisitionChannel">
          <el-select v-model="queryParams.acquisitionChannel" clearable placeholder="请选择获客渠道" style="width: 160px" @change="handleQuery">
            <el-option
              v-for="dict in dict.type.hkqd"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </div>
      <div v-if="activeFilterTags.length" class="active-filters">
        <span class="filter-label">当前筛选：</span>
        <el-tag
          v-for="tag in activeFilterTags"
          :key="tag.key"
          size="mini"
          closable
          @close="clearFilterTag(tag.key)"
        >{{ tag.label }}</el-tag>
      </div>
    </el-form>
    </div>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:app_customer:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:app_customer:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:app_customer:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['system:app_customer:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5">
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
            v-hasPermi="['system:app_customer:export']"
          >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <CustomerBitableTable
      :list="app_customerList"
      :loading="loading"
      :total="total"
      :dict="dict"
      :visible-column-props.sync="visibleColumnProps"
      :default-sort="defaultSort"
      @selection-change="handleSelectionChange"
      @row-click="handleDetail"
      @detail="handleDetail"
      @edit="handleUpdate"
      @delete="handleDelete"
      @show-default-columns="applyDefaultColumns"
      @reset-columns="applyDefaultColumns"
      @save-columns="saveColumnPrefs"
      @sort-change="handleSortChange"
      :loading-more="loadingMore"
      :is-all-loaded="isAllLoaded"
      @load-more="loadMore"
    />
    <!-- 客户资料详情对话框 -->
<!--    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" label-width="240px" :disabled="true">
        <el-row>
          <el-col :span="12">
            <el-form-item label="创建人" prop="createBy">
              <label>{{ form.createBy }}</label>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-dialog>-->
    <!-- 添加或修改客户资料对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="240px" :disabled="formDisabled">
        <el-form-item v-if="false" label="创建人" prop="createBy">
          <el-input v-model="form.createBy" disabled />
        </el-form-item>
        <el-form-item label="系统用户ID" prop="userId" v-if="false">
          <el-input v-model="form.userId" />
        </el-form-item>
        <el-form-item label="客户姓名" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户姓名" />
        </el-form-item>
        <el-form-item label="消费记录" prop="buyRecords">
          <el-input v-model="form.buyRecords" placeholder="请输入消费记录" />
        </el-form-item>
        <el-form-item label="所属站点" prop="deptId">
          <treeselect v-model="form.deptId" :options="deptOptions" :show-count="true" placeholder="请选择归属区县" />
        </el-form-item>
        <el-form-item label="康养顾问" prop="consultantId">
          <el-select v-model="form.consultantId" filterable clearable placeholder="请选择关联康养顾问" style="width:100%">
            <el-option
              v-for="item in consultantOptions"
              :key="item.consultantId"
              :label="consultantOptionLabel(item)"
              :value="item.consultantId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="客户编号" prop="customerNo">
          <el-input v-model="form.customerNo" placeholder="请输入客户编号" />
        </el-form-item>
        <el-form-item label="联系电话" prop="linkMobile">
          <el-input v-model="form.linkMobile" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="登记日期" prop="signTime">
          <el-date-picker clearable
            v-model="form.signTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择登记日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="是否进行回访" prop="returnVisit">
          <el-select v-model="form.returnVisit" placeholder="请选择是否进行回访">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <!-- 回访选填卡 -->
        <el-card v-if="form.returnVisit === 1" style="margin-bottom: 20px;">
        <el-form-item label="回访记录" prop="returnVisitRemark">
          <el-input v-model="form.returnVisitRemark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="第一次回访时间" prop="returnVisitFirst">
          <el-date-picker clearable
            v-model="form.returnVisitFirst"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择第一次回访时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="第二次回访时间" prop="returnVisitSecond">
          <el-date-picker clearable
            v-model="form.returnVisitSecond"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择第二次回访时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="最近一次回访时间" prop="returnVisitLast">
          <el-date-picker clearable
            v-model="form.returnVisitLast"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择最近一次回访时间">
          </el-date-picker>
        </el-form-item>
        </el-card>
        <el-form-item label="获客渠道" prop="acquisitionChannel">
          <el-select v-model="form.acquisitionChannel" placeholder="请选择获客渠道">
            <el-option
              v-for="dict in dict.type.hkqd"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="客户标签" prop="customerLabel">
          <el-select v-model="form.customerLabel" placeholder="请选择客户标签">
            <el-option
              v-for="dict in dict.type.khbq"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="是否进行客户信息录入" prop="customerInfo">
          <el-select v-model="form.customerInfo" placeholder="请选择是否进行客户信息录入">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <!-- 客户信息录入选填卡 -->
        <el-card v-if="form.customerInfo === '1'" style="margin-bottom: 20px;">
        <el-form-item label="身份证号" prop="idcard">
          <el-input v-model="form.idcard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-select v-model="form.sex" placeholder="请选择性别">
            <el-option
              v-for="dict in dict.type.sys_user_sex"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthday">
          <el-date-picker clearable
            v-model="form.birthday"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择出生日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="岁数" prop="age">
          <el-input v-model="form.age" placeholder="请输入岁数" />
        </el-form-item>
        <el-form-item label="民族" prop="nation">
          <el-select v-model="form.nation" placeholder="请选择民族">
            <el-option
              v-for="dict in dict.type.nation"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="是否持有特病卡" prop="haveSpecialCard">
          <el-select v-model="form.haveSpecialCard" placeholder="请选择是否持有特病卡">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="文化程度" prop="education">
          <el-select v-model="form.education" placeholder="请选择文化程度">
            <el-option
              v-for="dict in dict.type.whcd"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="宗教信仰" prop="religion">
          <el-select v-model="form.religion" placeholder="请选择宗教信仰">
            <el-option
              v-for="dict in dict.type.zjxy"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="婚姻状况" prop="marital">
          <el-select v-model="form.marital" placeholder="请选择婚姻状况">
            <el-option
              v-for="dict in dict.type.hyzk"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="现居住地址" prop="liveAddress">
          <el-input v-model="form.liveAddress" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="户口所在地" prop="idcardAddress">
          <el-input v-model="form.idcardAddress" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="居住情况" prop="liveInro">
          <el-input v-model="form.liveInro" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="家庭中有65岁及以上的人数" prop="familyGt65Count">
          <el-input v-model="form.familyGt65Count" placeholder="请输入家庭中有65岁及以上的人数" />
        </el-form-item>
        <el-form-item label="家中是行动不便的人数" prop="familyDwalkCount">
          <el-input v-model="form.familyDwalkCount" placeholder="请输入家中是行动不便的人数" />
        </el-form-item>
        <el-form-item label="联络人（1）姓名" prop="link1Name">
          <el-input v-model="form.link1Name" placeholder="请输入联络人" />
        </el-form-item>
        <el-form-item label="联络人（1）与老人的关系" prop="link1Relation">
          <el-select v-model="form.link1Relation" placeholder="请选择联络人">
            <el-option
              v-for="dict in dict.type.ykhgx"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="联络人（1）联系方式" prop="link1Mobile">
          <el-input v-model="form.link1Mobile" placeholder="请输入联络人" />
        </el-form-item>
        <el-form-item label="联络人（2）姓名" prop="link2Name">
          <el-input v-model="form.link2Name" placeholder="请输入联络人" />
        </el-form-item>
        <el-form-item label="联络人（2）与老人的关系" v-if="form.link2Name" prop="link2Relation">
          <el-select v-model="form.link2Relation" placeholder="请选择联络人">
            <el-option
              v-for="dict in dict.type.ykhgx"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="联络人（2）联系方式" v-if="form.link2Name" prop="link2Mobile">
          <el-input v-model="form.link2Mobile" placeholder="请输入联络人" />
        </el-form-item>
        <el-form-item label="信息提供者姓名" prop="infoPersonName">
          <el-input v-model="form.infoPersonName" placeholder="请输入信息提供者姓名" />
        </el-form-item>
        <el-form-item label="信息提供者与老人关系" prop="infoPersonRelation">
          <el-select v-model="form.infoPersonRelation" placeholder="请选择信息提供者与老人关系">
            <el-option
              v-for="dict in dict.type.ykhgx"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
          <el-form-item label="是否购买福地" prop="purchasedCemetery">
            <el-select v-model="form.purchasedCemetery" placeholder="请选择是否购买福地">
              <el-option
                v-for="dict in dict.type.common_is_not2"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-card>
        <el-form-item label="是否进行身体状况评估" prop="healthTest">
          <el-select v-model="form.healthTest" placeholder="请选择是否进行身体状况评估">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <!-- 身体状况评估选填卡 -->
        <el-card v-if="form.healthTest === '1'" style="margin-bottom: 20px;">
        <el-form-item label="疾病诊断-&gt;认知症" prop="diseaseDementia">
          <el-select v-model="form.diseaseDementia" placeholder="请选择疾病诊断-&gt;认知症">
            <el-option
              v-for="dict in dict.type.jbzdrzz"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="疾病诊断-&gt;精神疾病" prop="diseaseMental">
          <el-select v-model="form.diseaseMental" placeholder="请选择疾病诊断-&gt;精神疾病">
            <el-option
              v-for="dict in dict.type.jbzdjsjb"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
          <el-form-item label="疾病诊断-&gt;躯体疾病" prop="diseaseMental">
            <el-select v-model="form.diseaseMental" placeholder="请选择疾病诊断-&gt;精神疾病">
              <el-option
                v-for="dict in dict.type.jbzdqtjb"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              ></el-option>
            </el-select>
          </el-form-item>
        <el-form-item label="疾病诊断-&gt;其他疾病" prop="diseaseOther">
          <el-input v-model="form.diseaseOther" placeholder="请输入疾病诊断-&gt;其他疾病" />
        </el-form-item>
        <el-form-item label="有无长期服药|中医|理疗|中医茶饮的情况" prop="medicationLong">
          <el-select v-model="form.medicationLong" placeholder="请选择有无长期服药|中医|理疗|中医茶饮的情况">
            <el-option
              v-for="dict in dict.type.common_is_not2"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>

          <el-form-item  v-if="form.medicationLong === '1'" label="服用药品名称及使用方式及剂量" prop="medicationRemark">
            <el-input v-model="form.medicationRemark" placeholder="服用药品名称及使用方式及剂量" />
          </el-form-item>

        </el-card>
        <el-form-item label="是否每年体检" prop="checkUpYear">
          <el-select v-model="form.checkUpYear" placeholder="请选择是否每年体检">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="慢性病是否定期就诊" prop="chronicDiseaseCheck">
          <el-select v-model="form.chronicDiseaseCheck" placeholder="请选择慢性病是否定期就诊">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="自理能力" prop="selfAbility">
          <el-select v-model="form.selfAbility" placeholder="请选择自理能力">
            <el-option
              v-for="dict in dict.type.zlnl"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="是否进行成员评估" prop="membersEvaluate">
          <el-select v-model="form.membersEvaluate" placeholder="请选择是否进行成员评估">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <!-- 成员评估选填卡 -->
        <el-card v-if="form.membersEvaluate === '1'" style="margin-bottom: 20px;">
        <el-form-item label="子女情况" prop="children">
          <el-input v-model="form.children" placeholder="请输入子女情况" />
        </el-form-item>
        <el-form-item label="子女是否在当地工作" prop="childrenNearly">
          <el-select v-model="form.childrenNearly" placeholder="请选择子女是否在当地工作">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="有无照护者" prop="caregiver">
          <el-select v-model="form.caregiver" placeholder="请选择有无照护者">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="照护者数量" prop="caregiverCount">
          <el-input v-model="form.caregiverCount" placeholder="请输入照护者数量" />
        </el-form-item>
        <el-form-item label="照护者是否有照护经验" prop="caregiverExperience">
          <el-select v-model="form.caregiverExperience" placeholder="请选择照护者是否有照护经验">
            <el-option
              v-for="dict in dict.type.zhjy"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="照护内容" prop="careContent">
          <el-select v-model="form.careContent" placeholder="请选择照护内容">
            <el-option
              v-for="dict in dict.type.zhnr"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="照护时间" prop="careTimes">
          <el-select v-model="form.careTimes" placeholder="请选择照护时间">
            <el-option
              v-for="dict in dict.type.zhsj"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        </el-card>

        <el-form-item label="是否进行养老政策评估" prop="elderlyCareEvaluate">
          <el-select v-model="form.elderlyCareEvaluate" placeholder="请选择是否进行养老政策评估">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <!-- 养老政策评估选填卡 -->
        <el-card v-if="form.elderlyCareEvaluate === '1'" style="margin-bottom: 20px;">
        <el-form-item label="享有养老（助残）服务补贴" prop="elderlyCareSubsidy">
          <el-input v-model="form.elderlyCareSubsidy" placeholder="请输入享有养老（助残）服务补贴" />
        </el-form-item>
        <el-form-item label="是否享受长期护理保险" prop="insuranceLongCare">
          <el-select v-model="form.insuranceLongCare" placeholder="请选择是否享受长期护理保险">
            <el-option
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="医疗支付方式" prop="medicalPayMethod">
          <el-select v-model="form.medicalPayMethod" placeholder="请选择医疗支付方式">
            <el-option
              v-for="dict in dict.type.ylzhfs"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="月退休金|养老金" prop="pensionMonth">
          <el-input v-model="form.pensionMonth" placeholder="请输入月退休金|养老金" />
        </el-form-item>

          <el-form-item label="子女或其他补贴" prop="childPensionMonth">
            <el-input v-model="form.childPensionMonth" placeholder="请输入子女或其他补贴" />
          </el-form-item>
        </el-card>
<!--        <el-form-item label="是否进行长者养老需求评估报告" prop="elderlyCareReport">-->
<!--          <el-select v-model="form.elderlyCareReport" placeholder="请选择是否进行长者养老需求评估报告">-->
<!--            <el-option-->
<!--              v-for="dict in dict.type.common_is_not"-->
<!--              :key="dict.value"-->
<!--              :label="dict.label"-->
<!--              :value="dict.value"-->
<!--            ></el-option>-->
<!--          </el-select>-->
<!--        </el-form-item>-->
        <!-- 养老需求评估报告选填卡 -->
<!--        <el-card v-if="form.elderlyCareReport === '1'" style="margin-bottom: 20px;">-->
<!--        <el-form-item label="机构托养" prop="organizationCare">-->
<!--          <el-input v-model="form.organizationCare" placeholder="请输入机构托养" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="综合为老服务中心" prop="elderlyService">-->
<!--          <el-input v-model="form.elderlyService" placeholder="请输入综合为老服务中心" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="康复服务" prop="rehabilitationService">-->
<!--          <el-input v-model="form.rehabilitationService" placeholder="请输入康复服务" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="医疗机构" prop="medicalInstitution">-->
<!--          <el-input v-model="form.medicalInstitution" placeholder="请输入医疗机构" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="居家服务" prop="familyService">-->
<!--          <el-input v-model="form.familyService" placeholder="请输入居家服务" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="适老化智能化养老" prop="elderlyCareAi">-->
<!--          <el-input v-model="form.elderlyCareAi" placeholder="请输入适老化智能化养老" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="中医服务" prop="chineseMedicalService">-->
<!--          <el-input v-model="form.chineseMedicalService" placeholder="请输入中医服务" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="非医疗性护理" prop="careToDoor">-->
<!--          <el-input v-model="form.careToDoor" placeholder="请输入非医疗性护理" />-->
<!--        </el-form-item>-->
<!--        </el-card>-->
        <el-form-item label="附件" prop="attach">
          <file-upload v-model="form.attach"/>
        </el-form-item>
        <el-form-item label="养老顾问(外部)" prop="consultant">
          <el-input v-model="form.consultant" placeholder="请输入养老顾问" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="!formDisabled">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 示例：专用详情对话框 -->
    <el-dialog :title="title" :visible.sync="detailOpen" style="min-width: 960px" append-to-body>
      <el-scrollbar style="height: 500px;">
        <el-descriptions :column="2" border :label-style="{ width: '150px',height:'30px'}" content-style="width:330px">
          <el-descriptions-item label="客户姓名">{{ form.customerName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ form.linkMobile }}</el-descriptions-item>
          <el-descriptions-item label="所属站点">{{ form.deptName }} </el-descriptions-item>
          <el-descriptions-item label="客户编号">{{ form.customerNo }}</el-descriptions-item>
          <el-descriptions-item label="登记日期">{{ parseTime(form.signTime, '{y}-{m}-{d}') }}</el-descriptions-item>
          <el-descriptions-item label="康养顾问">{{ consultantDisplayName(form) }}</el-descriptions-item>
          <el-descriptions-item label="顾问ID">{{ form.consultantId || '-' }}</el-descriptions-item>

          <!-- 基本信息 -->
          <el-descriptions-item label="性别">
            <dict-tag :options="dict.type.sys_user_sex" :value="form.sex"/>
          </el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ parseTime(form.birthday, '{y}-{m}-{d}') }}</el-descriptions-item>
          <el-descriptions-item label="岁数">{{ form.age }}</el-descriptions-item>
          <el-descriptions-item label="民族">
            <dict-tag :options="dict.type.nation" :value="form.nation"/>
          </el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ form.idcard }}</el-descriptions-item>
          <el-descriptions-item label="婚姻状况">
            <dict-tag :options="dict.type.hyzk" :value="form.marital"/>
          </el-descriptions-item>
          <el-descriptions-item label="文化程度">
            <dict-tag :options="dict.type.whcd" :value="form.education"/>
          </el-descriptions-item>
          <el-descriptions-item label="宗教信仰">
            <dict-tag :options="dict.type.zjxy" :value="form.religion"/>
          </el-descriptions-item>

          <!-- 回访信息 -->
          <el-descriptions-item label="是否进行回访">
            <dict-tag :options="dict.type.common_is_not" :value="form.returnVisit"/>
          </el-descriptions-item>
          <el-descriptions-item label="回访记录">{{ form.returnVisitRemark }}</el-descriptions-item>
          <el-descriptions-item label="第一次回访时间">{{ parseTime(form.returnVisitFirst, '{y}-{m}-{d}') }}</el-descriptions-item>
          <el-descriptions-item label="第二次回访时间">{{ parseTime(form.returnVisitSecond, '{y}-{m}-{d}') }}</el-descriptions-item>
          <el-descriptions-item label="最近一次回访时间">{{ parseTime(form.returnVisitLast, '{y}-{m}-{d}') }}</el-descriptions-item>

          <!-- 客户分类信息 -->
          <el-descriptions-item label="获客渠道">
            <dict-tag :options="dict.type.hkqd" :value="form.acquisitionChannel"/>
          </el-descriptions-item>
          <el-descriptions-item label="客户标签">
            <dict-tag :options="dict.type.khbq" :value="form.customerLabel"/>
          </el-descriptions-item>

          <!-- 健康状况 -->
          <el-descriptions-item label="是否持有特病卡">
            <dict-tag :options="dict.type.common_is_not" :value="form.haveSpecialCard"/>
          </el-descriptions-item>
          <el-descriptions-item label="是否进行身体状况评估">
            <dict-tag :options="dict.type.common_is_not" :value="form.healthTest"/>
          </el-descriptions-item>
          <el-descriptions-item label="疾病诊断->认知症">
            <dict-tag :options="dict.type.jbzdrzz" :value="form.diseaseDementia"/>
          </el-descriptions-item>
          <el-descriptions-item label="疾病诊断->精神疾病">
            <dict-tag :options="dict.type.jbzdjsjb" :value="form.diseaseMental"/>
          </el-descriptions-item>
          <el-descriptions-item label="疾病诊断->其他疾病">{{ form.diseaseOther }}</el-descriptions-item>
          <el-descriptions-item label="有无长期服药">
            <dict-tag :options="dict.type.common_is_not2" :value="form.medicationLong"/>
          </el-descriptions-item>
          <el-descriptions-item label="服用药品名称及使用方式">{{ form.medicationRemark }}</el-descriptions-item>
          <el-descriptions-item label="是否每年体检">
            <dict-tag :options="dict.type.common_is_not" :value="form.checkUpYear"/>
          </el-descriptions-item>
          <el-descriptions-item label="慢性病是否定期就诊">
            <dict-tag :options="dict.type.common_is_not" :value="form.chronicDiseaseCheck"/>
          </el-descriptions-item>
          <el-descriptions-item label="自理能力">
            <dict-tag :options="dict.type.zlnl" :value="form.selfAbility"/>
          </el-descriptions-item>

          <!-- 居住和家庭信息 -->
          <el-descriptions-item label="现居住地址" :span="2">{{ form.liveAddress }}</el-descriptions-item>
          <el-descriptions-item label="户口所在地" :span="2">{{ form.idcardAddress }}</el-descriptions-item>
          <el-descriptions-item label="居住情况" :span="2">{{ form.liveInro }}</el-descriptions-item>
          <el-descriptions-item label="住宅类型">
            <dict-tag :options="dict.type.common_is_not" :value="form.houseType"/>
          </el-descriptions-item>
          <el-descriptions-item label="家庭中有65岁及以上的人数">{{ form.familyGt65Count }}</el-descriptions-item>
          <el-descriptions-item label="家中是行动不便的人数">{{ form.familyDwalkCount }}</el-descriptions-item>

          <!-- 联络人信息 -->
          <el-descriptions-item label="联络人（1）姓名">{{ form.link1Name }}</el-descriptions-item>
          <el-descriptions-item label="联络人（1）关系">
            <dict-tag :options="dict.type.ykhgx" :value="form.link1Relation"/>
          </el-descriptions-item>
          <el-descriptions-item label="联络人（1）联系方式">{{ form.link1Mobile }}</el-descriptions-item>
          <el-descriptions-item label="联络人（2）姓名">{{ form.link2Name }}</el-descriptions-item>
          <el-descriptions-item label="联络人（2）关系">
            <dict-tag :options="dict.type.ykhgx" :value="form.link2Relation"/>
          </el-descriptions-item>
          <el-descriptions-item label="联络人（2）联系方式">{{ form.link2Mobile }}</el-descriptions-item>

          <!-- 信息提供者 -->
          <el-descriptions-item label="信息提供者姓名">{{ form.infoPersonName }}</el-descriptions-item>
          <el-descriptions-item label="信息提供者关系">
            <dict-tag :options="dict.type.ykhgx" :value="form.infoPersonRelation"/>
          </el-descriptions-item>

          <!-- 成员评估 -->
          <el-descriptions-item label="是否进行成员评估">
            <dict-tag :options="dict.type.common_is_not" :value="form.membersEvaluate"/>
          </el-descriptions-item>
          <el-descriptions-item label="子女情况">{{ form.children }}</el-descriptions-item>
          <el-descriptions-item label="子女是否在当地工作">
            <dict-tag :options="dict.type.common_is_not" :value="form.childrenNearly"/>
          </el-descriptions-item>
          <el-descriptions-item label="与家庭成员的情感关系">{{ form.membersRelation }}</el-descriptions-item>
          <el-descriptions-item label="有无照护者">
            <dict-tag :options="dict.type.common_is_not" :value="form.caregiver"/>
          </el-descriptions-item>
          <el-descriptions-item label="照护者数量">{{ form.caregiverCount }}</el-descriptions-item>
          <el-descriptions-item label="照护者是否有照护经验">
            <dict-tag :options="dict.type.zhjy" :value="form.caregiverExperience"/>
          </el-descriptions-item>
          <el-descriptions-item label="照护内容">
            <dict-tag :options="dict.type.zhnr" :value="form.careContent"/>
          </el-descriptions-item>
          <el-descriptions-item label="照护时间">
            <dict-tag :options="dict.type.zhsj" :value="form.careTimes"/>
          </el-descriptions-item>

          <!-- 养老政策评估 -->
          <el-descriptions-item label="是否进行养老政策评估">
            <dict-tag :options="dict.type.common_is_not" :value="form.elderlyCareEvaluate"/>
          </el-descriptions-item>
          <el-descriptions-item label="享有养老（助残）服务补贴">{{ form.elderlyCareSubsidy }}</el-descriptions-item>
          <el-descriptions-item label="是否享受长期护理保险">
            <dict-tag :options="dict.type.common_is_not" :value="form.insuranceLongCare"/>
          </el-descriptions-item>
          <el-descriptions-item label="医疗支付方式">
            <dict-tag :options="dict.type.ylzhfs" :value="form.medicalPayMethod"/>
          </el-descriptions-item>
          <el-descriptions-item label="月退休金|养老金">{{ form.pensionMonth }}</el-descriptions-item>
          <el-descriptions-item label="子女或其他补贴">{{ form.childPensionMonth }}</el-descriptions-item>

          <!-- 需求评估 -->
          <el-descriptions-item label="是否进行长者养老需求评估报告">
            <dict-tag :options="dict.type.common_is_not" :value="form.elderlyCareReport"/>
          </el-descriptions-item>
          <el-descriptions-item label="机构托养">{{ form.organizationCare }}</el-descriptions-item>
          <el-descriptions-item label="综合为老服务中心">{{ form.elderlyService }}</el-descriptions-item>
          <el-descriptions-item label="康复服务">{{ form.rehabilitationService }}</el-descriptions-item>
          <el-descriptions-item label="医疗机构">{{ form.medicalInstitution }}</el-descriptions-item>
          <el-descriptions-item label="居家服务">{{ form.familyService }}</el-descriptions-item>
          <el-descriptions-item label="适老化智能化养老">{{ form.elderlyCareAi }}</el-descriptions-item>
          <el-descriptions-item label="中医服务">{{ form.chineseMedicalService }}</el-descriptions-item>
          <el-descriptions-item label="非医疗性护理">{{ form.careToDoor }}</el-descriptions-item>

          <!-- 其他信息 -->
          <el-descriptions-item label="是否购买福地">
            <dict-tag :options="dict.type.common_is_not2" :value="form.purchasedCemetery"/>
          </el-descriptions-item>
          <el-descriptions-item label="养老顾问（外部）">{{ form.consultant }}</el-descriptions-item>
          <el-descriptions-item label="消费记录" :span="2">{{ form.buyRecords }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ form.remark }}</el-descriptions-item>
          <el-descriptions-item label="附件" :span="2">{{ form.attach }}</el-descriptions-item>
        </el-descriptions>
      </el-scrollbar>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancel">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的客户数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApp_customer, getApp_customer, delApp_customer, addApp_customer, updateApp_customer } from "@/api/system/app_customer";
import { listApp_consultant } from "@/api/system/app_consultant";
import CustomerBitableTable from './CustomerBitableTable.vue';
import { DEFAULT_VISIBLE_PROPS, loadVisibleColumnProps, saveVisibleColumnProps } from './customerColumns';

import {deptTreeSelect} from "@/api/system/user";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { listDept } from "@/api/system/dept";
import {getToken} from "@/utils/auth";

export default {
  name: "App_customer",
  dicts: ['common_is_not', 'sys_common_status', 'sys_user_sex', 'hkqd', 'khbq', 'whcd', 'zjxy', 'hyzk', 'ykhgx', 'jbzdrzz', 'jbzdjsjb', 'jbzdqijb', 'common_is_not2', 'zlnl', 'zhjy', 'zhnr', 'zhsj', 'ylzhfs', 'nation', 'ylfwbt'],
  components: {
    Treeselect,
    CustomerBitableTable
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 客户资料表格数据
      app_customerList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        orderByColumn: 'signTime',
        isAsc: 'descending',
        userId: null,
        customerName: null,
        buyRecords: null,
        deptId: null,
        customerNo: null,
        linkMobile: null,
        signTime: null,
        insuranceEvaStatus: null,
        returnVisit: null,
        returnVisitRemark: null,
        returnVisitFirst: null,
        returnVisitSecond: null,
        returnVisitLast: null,
        acquisitionChannel: null,
        customerLabel: null,
        customerInfo: null,
        customerGoods: null,
        consultantId: null,
        idcard: null,
        sex: null,
        birthday: null,
        age: null,
        nation: null,
        haveSpecialCard: null,
        education: null,
        religion: null,
        marital: null,
        liveAddress: null,
        idcardAddress: null,
        liveInro: null,
        houseType: null,
        familyGt65Count: null,
        familyDwalkCount: null,
        link1Name: null,
        link1Relation: null,
        link1Mobile: null,
        link2Name: null,
        link2Relation: null,
        link2Mobile: null,
        infoPersonName: null,
        infoPersonRelation: null,
        healthTest: null,
        diseaseDementia: null,
        diseaseMental: null,
        diseaseOther: null,
        medicationLong: null,
        checkUpYear: null,
        chronicDiseaseCheck: null,
        selfAbility: null,
        membersEvaluate: null,
        children: null,
        childrenNearly: null,
        membersRelation: null,
        caregiver: null,
        caregiverCount: null,
        caregiverExperience: null,
        careContent: null,
        careTimes: null,
        elderlyCareEvaluate: null,
        elderlyCareSubsidy: null,
        insuranceLongCare: null,
        medicalPayMethod: null,
        medicalPayMethodRemark: null,
        pensionMonth: null,
        elderlyCareReport: null,
        organizationCare: null,
        elderlyService: null,
        rehabilitationService: null,
        medicalInstitution: null,
        familyService: null,
        elderlyCareAi: null,
        chineseMedicalService: null,
        careToDoor: null,
        purchasedCemetery: null,
        attach: null,
        consultant: null,
        medicalCare: null,
        parentRecord: null,
        status: null
      },
      // 其他数据...
      formDisabled: false,
      // 部门名称
      deptName: undefined,
      detailOpen: false,
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      },

      // 导入参数
      upload: {
        // 是否显示弹出层（导入）
        open: false,
        // 弹出层标题（导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/system/app_customer/importData"
      },

      // 分站树选项
      deptOptions: [],
      // 康养顾问选项
      consultantOptions: [],
      // 多维表格可见列
      visibleColumnProps: loadVisibleColumnProps(),
      loadingMore: false,
      isAllLoaded: false,
      searchKeyword: '',
      showAdvancedSearch: false,
      signDateRange: [],
      defaultSort: { prop: 'signTime', order: 'descending' },
      debouncedQuery: null,
    };
  },
  computed: {
    activeFilterTags() {
      const tags = []
      if (this.searchKeyword) {
        tags.push({ key: 'keyword', label: `关键词：${this.searchKeyword}` })
      }
      if (this.queryParams.consultantId) {
        const found = this.consultantOptions.find(item => item.consultantId === this.queryParams.consultantId)
        tags.push({ key: 'consultantId', label: `顾问：${found ? found.consultantName : this.queryParams.consultantId}` })
      }
      if (this.signDateRange && this.signDateRange.length === 2) {
        tags.push({ key: 'signDateRange', label: `登记：${this.signDateRange[0]} ~ ${this.signDateRange[1]}` })
      }
      if (this.queryParams.customerName) {
        tags.push({ key: 'customerName', label: `姓名：${this.queryParams.customerName}` })
      }
      if (this.queryParams.customerNo) {
        tags.push({ key: 'customerNo', label: `编号：${this.queryParams.customerNo}` })
      }
      if (this.queryParams.idcard) {
        tags.push({ key: 'idcard', label: `身份证：${this.queryParams.idcard}` })
      }
      if (this.queryParams.linkMobile) {
        tags.push({ key: 'linkMobile', label: `电话：${this.queryParams.linkMobile}` })
      }
      if (this.queryParams.customerLabel) {
        const found = (this.dict.type.khbq || []).find(item => item.value === this.queryParams.customerLabel)
        tags.push({ key: 'customerLabel', label: `标签：${found ? found.label : this.queryParams.customerLabel}` })
      }
      if (this.queryParams.acquisitionChannel) {
        const found = (this.dict.type.hkqd || []).find(item => item.value === this.queryParams.acquisitionChannel)
        tags.push({ key: 'acquisitionChannel', label: `渠道：${found ? found.label : this.queryParams.acquisitionChannel}` })
      }
      return tags
    }
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    }
  },
  created() {
    this.debouncedQuery = this.debounce(this.handleQuery, 400)
    this.getList();
    this.getDeptTree();
    this.getConsultantOptions();
  },
  methods: {
    debounce(fn, delay) {
      let timer = null
      return function(...args) {
        clearTimeout(timer)
        timer = setTimeout(() => fn.apply(this, args), delay)
      }
    },
    buildListQueryParams() {
      const params = this.addDateRange({ ...this.queryParams }, this.signDateRange, 'SignTime')
      if (this.searchKeyword && this.searchKeyword.trim()) {
        params.params = { ...(params.params || {}), keyword: this.searchKeyword.trim() }
      }
      return params
    },
    clearFilterTag(key) {
      switch (key) {
        case 'keyword':
          this.searchKeyword = ''
          break
        case 'consultantId':
          this.queryParams.consultantId = null
          break
        case 'signDateRange':
          this.signDateRange = []
          break
        case 'customerName':
          this.queryParams.customerName = null
          break
        case 'customerNo':
          this.queryParams.customerNo = null
          break
        case 'idcard':
          this.queryParams.idcard = null
          break
        case 'linkMobile':
          this.queryParams.linkMobile = null
          break
        case 'customerLabel':
          this.queryParams.customerLabel = null
          break
        case 'acquisitionChannel':
          this.queryParams.acquisitionChannel = null
          break
        default:
          break
      }
      this.handleQuery()
    },
    applyDefaultColumns() {
      this.visibleColumnProps = [...DEFAULT_VISIBLE_PROPS];
    },
    saveColumnPrefs() {
      saveVisibleColumnProps(this.visibleColumnProps);
      this.$modal.msgSuccess('列配置已保存');
    },
    consultantOptionLabel(item) {
      const name = item.consultantName || '未命名';
      const mobile = item.mobile ? ` · ${item.mobile}` : '';
      return `${name} (ID:${item.consultantId})${mobile}`;
    },
    consultantDisplayName(form) {
      if (form.consultantName) return form.consultantName;
      const found = this.consultantOptions.find(item => item.consultantId === form.consultantId);
      return found ? found.consultantName : '-';
    },
    getConsultantOptions() {
      listApp_consultant({ pageNum: 1, pageSize: 9999 }).then(response => {
        this.consultantOptions = response.rows || [];
      });
    },
    getList(refresh = true) {
      if (refresh) {
        this.queryParams.pageNum = 1
        this.isAllLoaded = false
        this.loading = true
      } else {
        this.loadingMore = true
      }
      listApp_customer(this.buildListQueryParams()).then(response => {
        const rows = response.rows || []
        if (refresh) {
          this.app_customerList = rows
        } else {
          this.app_customerList = [...this.app_customerList, ...rows]
        }
        this.total = response.total || 0
        this.isAllLoaded = this.app_customerList.length >= this.total || rows.length < this.queryParams.pageSize
      }).catch(() => {
        this.$modal.msgError('数据加载失败')
      }).finally(() => {
        this.loading = false
        this.loadingMore = false
      })
    },
    loadMore() {
      if (this.isAllLoaded || this.loadingMore || this.loading) {
        return
      }
      this.queryParams.pageNum += 1
      this.getList(false)
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data;
      });
    },

    // /** 转换分站数据结构 */
    // normalizerDept(node) {
    //   if (node.children && !node.children.length) {
    //     delete node.children;
    //   }
    //   return {
    //     id: node.deptId,
    //     label: node.deptName,
    //     children: node.children
    //   };
    // },
    // /** 查询部门下拉树结构 */
    // getDeptTreeselect() {
    //   listDept().then(response => {
    //     console.log(response.data)
    //     this.deptOptions = [];
    //     const data = { deptId: 0, deptName: '无站点', children: [] };
    //     data.children = this.handleTree(response.data, "deptId", "parentId");
    //     this.deptOptions.push(data);
    //   });
    // },

    /** 查询部门列表 */
    getDeptList() {
      // this.loading = true;
      listDept(this.queryParams).then(response => {
        this.deptList = this.handleTree(response.data, "deptId");
        // this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.detailOpen = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        customerId: null,
        userId: null,
        customerName: null,
        buyRecords: null,
        deptId: null,
        deptName: null,
        customerNo: null,
        linkMobile: null,
        signTime: null,
        insuranceEvaStatus: null,
        returnVisit: null,
        returnVisitRemark: null,
        returnVisitFirst: null,
        returnVisitSecond: null,
        returnVisitLast: null,
        acquisitionChannel: null,
        customerLabel: null,
        customerInfo: null,
        customerGoods: null,
        consultantId: null,
        idcard: null,
        sex: null,
        birthday: null,
        age: null,
        nation: null,
        haveSpecialCard: null,
        education: null,
        religion: null,
        marital: null,
        liveAddress: null,
        idcardAddress: null,
        liveInro: null,
        houseType: null,
        familyGt65Count: null,
        familyDwalkCount: null,
        link1Name: null,
        link1Relation: null,
        link1Mobile: null,
        link2Name: null,
        link2Relation: null,
        link2Mobile: null,
        infoPersonName: null,
        infoPersonRelation: null,
        healthTest: null,
        diseaseDementia: null,
        diseaseMental: null,
        diseaseOther: null,
        medicationLong: null,
        checkUpYear: null,
        chronicDiseaseCheck: null,
        selfAbility: null,
        membersEvaluate: null,
        children: null,
        childrenNearly: null,
        membersRelation: null,
        caregiver: null,
        caregiverCount: null,
        caregiverExperience: null,
        careContent: null,
        careTimes: null,
        elderlyCareEvaluate: null,
        elderlyCareSubsidy: null,
        insuranceLongCare: null,
        medicalPayMethod: null,
        medicalPayMethodRemark: null,
        pensionMonth: null,
        elderlyCareReport: null,
        organizationCare: null,
        elderlyService: null,
        rehabilitationService: null,
        medicalInstitution: null,
        familyService: null,
        elderlyCareAi: null,
        chineseMedicalService: null,
        careToDoor: null,
        purchasedCemetery: null,
        attach: null,
        consultant: null,
        medicalCare: null,
        parentRecord: null,
        remark: null,
        status: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        delFlag: null,
        detailOpen: false
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList(true);
    },
    /** 排序触发事件 */
    handleSortChange(column) {
      if (!column.prop || !column.order) {
        this.queryParams.orderByColumn = 'signTime';
        this.queryParams.isAsc = 'descending';
        this.defaultSort = { prop: 'signTime', order: 'descending' };
      } else {
        this.queryParams.orderByColumn = column.prop;
        this.queryParams.isAsc = column.order;
        this.defaultSort = { prop: column.prop, order: column.order };
      }
      this.getList(true);
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.searchKeyword = ''
      this.signDateRange = []
      this.showAdvancedSearch = false
      this.queryParams.orderByColumn = 'signTime'
      this.queryParams.isAsc = 'descending'
      this.defaultSort = { prop: 'signTime', order: 'descending' }
      this.resetForm("queryForm");
      this.getList(true);
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.customerId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      // this.getDeptTreeselect();
      this.open = true;
      this.title = "添加客户资料";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      // this.getDeptTreeselect();
      const customerId = row.customerId || this.ids
      getApp_customer(customerId).then(response => {
        this.form = response.data;
        this.open = true;
        this.formDisabled = false; // 确保修改时表单可编辑
        this.title = "修改客户资料";
      });
    },
    /** 详情按钮操作 */
    handleDetail(row) {
      //this.reset();
      // this.getDeptTreeselect();
      const customerId = row.customerId || this.ids
      getApp_customer(customerId).then(response => {
        this.form = response.data;
        //this.formDisabled = true; // 设置表单为只读
        //this.open = true;
        this.detailOpen = true;
        this.title = "客户资料详情";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.customerId != null) {
            updateApp_customer(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_customer(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const customerIds = row.customerId || this.ids;
      this.$modal.confirm('是否确认删除客户资料编号为"' + customerIds + '"的数据项？').then(function() {
        return delApp_customer(customerIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_customer/export', {
        ...this.buildListQueryParams()
      }, `客户数据_导出_${new Date().getTime()}.xlsx`)
    },

    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "客户导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('system/app_customer/importTemplate', {
      }, `客户数据_导出模板_${new Date().getTime()}.xlsx`)
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    }
  }
};
</script>
<style scoped lang="scss">
@import './customer-table.scss';

.customer-page {
  ::v-deep .pagination-container {
    margin-top: 12px;
  }
}

.customer-search-form {
  .search-primary,
  .search-advanced {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .search-advanced {
    padding-top: 4px;
    border-top: 1px dashed #ebeef5;
    margin-top: 4px;
  }

  .keyword-item {
    ::v-deep .el-input-group__append {
      padding: 0 12px;
      background: #409eff;
      border-color: #409eff;
      color: #fff;
    }
  }

  .active-filters {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    padding: 8px 0 4px;
    border-top: 1px solid #f0f2f5;
    margin-top: 8px;

    .filter-label {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>
