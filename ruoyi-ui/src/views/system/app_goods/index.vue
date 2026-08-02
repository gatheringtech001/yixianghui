<template>
  <div class="app-container goods-page">
    <div class="filter-card" v-show="showSearch">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px" class="goods-search-form">
      <div class="search-primary">
        <el-form-item label="快捷搜索" class="keyword-item">
          <el-input
            v-model="searchKeyword"
            placeholder="商品名称 / 标签 / 简介"
            clearable
            style="width: 280px"
            @keyup.enter.native="handleQuery"
            @input="debouncedQuery"
            @clear="handleQuery"
          >
            <el-button slot="append" icon="el-icon-search" @click="handleQuery"></el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="商品类型" prop="goodsType">
          <el-select v-model="queryParams.goodsType" placeholder="请选择商品类型" clearable style="width: 140px" @change="handleQuery">
            <el-option v-for="dict in dict.type.goods_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择商品状态" clearable style="width: 140px" @change="handleQuery">
            <el-option v-for="dict in dict.type.goods_status" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="首页热推" prop="isHot">
          <el-select v-model="queryParams.isHot" placeholder="是否首页热门" clearable style="width: 140px" @change="handleQuery">
            <el-option v-for="dict in dict.type.common_is_not" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
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
        <el-form-item label="所属分站" prop="deptId">
          <treeselect
            v-model="queryParams.deptId"
            :options="deptOptions"
            :normalizer="normalizerDept"
            placeholder="选择所属分站"
            style="width: 220px"
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <treeselect
            v-model="queryParams.categoryId"
            :options="app_goods_categoryOptions"
            :normalizer="normalizerCategory"
            placeholder="选择所属分类"
            style="width: 220px"
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item label="商品名称" prop="goodsName">
          <el-input v-model="queryParams.goodsName" placeholder="请输入商品名称" clearable @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="商品标签" prop="tags">
          <el-input v-model="queryParams.tags" placeholder="请输入商品标签" clearable @keyup.enter.native="handleQuery" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['system:app_goods:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
                   v-hasPermi="['system:app_goods:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
                   v-hasPermi="['system:app_goods:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                   v-hasPermi="['system:app_goods:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      ref="goodsTable"
      v-loading="loading"
      :data="app_goodsList"
      row-key="goodsId"
      :default-sort="defaultSort"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
      :fit="true"
      style="width: 100%;min-width: 400px"
      max-height="calc(100vh - 320px)"
    >
      <el-table-column type="selection" width="55" align="center" fixed="left" />
      <el-table-column label="商品id" align="center" prop="goodsId" v-if="false" />
      <el-table-column label="所属分站" align="center" prop="deptId" v-if="false" />
      <el-table-column label="所属分站" align="center" prop="deptName" min-width="100" :resizable="true"
                       show-overflow-tooltip />
      <el-table-column label="商品名称" align="center" prop="goodsName" min-width="200" fixed="left" :resizable="true"
                       show-overflow-tooltip sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column label="商品类型" align="center" prop="goodsType" :resizable="true">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.goods_type" :value="scope.row.goodsType" />
        </template>
      </el-table-column>
      <el-table-column label="所属分类" align="center" prop="categoryId" :resizable="true" />
      <!--      <el-table-column label="祖级分类" align="center" prop="categoryIds" />-->
      <el-table-column label="封面图片" align="center" prop="goodsCover" width="100" :resizable="true">
        <template slot-scope="scope">
          <image-preview :src="scope.row.goodsCover" :width="50" :height="50" />
        </template>
      </el-table-column>
      <!--      <el-table-column label="轮播图" align="center" prop="goodsImages" width="100">-->
      <!--        <template slot-scope="scope">-->
      <!--          <image-preview :src="scope.row.goodsImages" :width="50" :height="50"/>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <!--      <el-table-column label="商品简介" align="center" prop="description" />-->
      <el-table-column label="商品标签" align="center" prop="tags" min-width="120" :resizable="true"
                       show-overflow-tooltip />
      <el-table-column label="价格" align="center" prop="price" :resizable="true" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column label="会员价格" align="center" prop="vipPrice" :resizable="true" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column label="单位" align="center" prop="unit" :resizable="true" />
      <!--      <el-table-column label="规格说明" align="center" prop="specifications" />-->
      <!--      <el-table-column label="库存" align="center" prop="stock" />-->
      <!--      <el-table-column label="是否置顶" align="center" prop="isTop">-->
      <!--        <template slot-scope="scope">-->
      <!--          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isTop"/>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <el-table-column label="是否首页热门" align="center" prop="isHot" :resizable="true">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isHot" />
        </template>
      </el-table-column>
      <!--      <el-table-column label="分类属性id集合" align="center" prop="attrIds" />-->
      <!--      <el-table-column label="分类属性值集合" align="center" prop="attrValues" />-->
      <!--      <el-table-column label="是否多规格" align="center" prop="isSku">-->
      <!--        <template slot-scope="scope">-->
      <!--          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isSku"/>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <!--      <el-table-column label="推荐奖励类型" align="center" prop="awardType">-->
      <!--        <template slot-scope="scope">-->
      <!--          <dict-tag :options="dict.type.award_type" :value="scope.row.awardType"/>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <!--      <el-table-column label="父级奖励比例" align="center" prop="awardParentRatio" />-->
      <!--      <el-table-column label="祖级奖励比例" align="center" prop="awardGrandParentRatio" />-->
      <el-table-column label="奖励金币" align="center" prop="awardGolden" :resizable="true" />
      <!--      <el-table-column label="运费" align="center" prop="expressFee" />-->
      <!--      <el-table-column label="重量(克)" align="center" prop="weight" />-->
      <!--      <el-table-column label="阅读次数" align="center" prop="viewCount" />-->
      <el-table-column label="销量" align="center" prop="saleCount" :resizable="true" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" sortable="custom" :sort-orders="['descending', 'ascending']">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="商品状态" align="center" prop="status" :resizable="true">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.goods_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right">
        <template slot-scope="scope">
          <div class="action-dropdown">
            <el-dropdown>
              <el-button type="primary" size="small">
                操作<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <!--                <el-dropdown-item @click.native="handleSku(scope.row)"-->
                <!--                                  icon="el-icon-info"-->
                <!--                                  v-has-permi="['system:app_goods:edit']" v-if="false">属性配置</el-dropdown-item>-->
                <el-dropdown-item @click.native="handleSku(scope.row)" v-if="false" icon="el-icon-info"
                                  v-has-permi="['system:app_goods:edit']">多规格</el-dropdown-item>
                <el-dropdown-item @click.native="handleUpdate(scope.row)" icon="el-icon-edit"
                                  v-has-permi="['system:app_goods:edit']">修改</el-dropdown-item>
                <el-dropdown-item @click.native="handleDelete(scope.row)" icon="el-icon-delete"
                                  v-hasPermi="['system:app_goods:remove']">删除</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                @pagination="getList" />

    <!-- 添加或修改商品对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="900px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <!--        <el-form-item label="祖级分类" prop="categoryIds">-->
        <!--          <el-input v-model="form.categoryIds" placeholder="请输入祖级分类" />-->
        <!--        </el-form-item>-->
        <!--        <el-form-item label="所属分站" prop="deptId">-->
        <!--          <el-input v-model="form.deptId" placeholder="请输入所属 分站" />-->
        <!--        </el-form-item>-->
        <!--        <el-row>-->
        <el-form-item label="所属分站" prop="deptId">
          <treeselect v-model="form.deptId" :options="deptOptions" :normalizer="normalizerDept" placeholder="选择所属分站" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <treeselect v-model="form.categoryId" :options="app_goods_categoryOptions" :normalizer="normalizerCategory"
                      placeholder="请选择所属分类" />
        </el-form-item>
        <el-form-item label="商品名称" prop="goodsName">
          <el-input v-model="form.goodsName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="封面图片" prop="goodsCover">
          <image-upload v-model="form.goodsCover" />
        </el-form-item>
        <el-form-item label="轮播图" prop="goodsImages">
          <image-upload v-model="form.goodsImages" />
        </el-form-item>
        <el-form-item label="商品简介" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="商品标签" prop="tags">
          <el-input v-model="form.tags" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="商品类型" prop="goodsType">
          <el-radio-group v-model="form.goodsType">
            <el-radio v-for="dict in dict.type.goods_type" :key="dict.value"
                      :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!isEducationGoods" label="是否多规格" prop="isSku">
          <el-radio-group v-model="form.isSku">
            <el-radio v-for="dict in dict.type.common_is_not" :key="dict.value"
                      :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 多规格编辑处-start -->
        <!--
        获取当前商品的规格列表、新增规格、删除规格
        获取当前规格的选项列表、新增选项、删除选项
        获取当前商品的规格选项组合列表、新增规格选项组合、删除规格选项组合
        -->
        <el-form-item v-if="form.isSku === 1 && !isEducationGoods" label="起始价格" prop="price">
          <el-input v-model="form.price" placeholder="请输入起始价格" />
        </el-form-item>
        <el-alert v-if="false" title="多规格配置,在创建商品后进行详细配置" style="margin-bottom: 20px;" type="success" :closable="false">
        </el-alert>

        <!-- 多规格编辑处-end -->

        <!-- 单规格编辑处-start -->
        <el-card v-if="form.isSku === 0 || isEducationGoods" style="margin-bottom: 20px;">
          <el-form-item label="价格" prop="price">
            <el-input v-model="form.price" placeholder="请输入价格" />
          </el-form-item>
          <el-form-item label="会员价格" prop="vipPrice">
            <el-input v-model="form.vipPrice" placeholder="请输入会员价格" />
          </el-form-item>
          <el-form-item label="单位" prop="unit">
            <el-input v-model="form.unit" :placeholder="isEducationGoods ? '如：期' : '请输入单位'" />
          </el-form-item>
          <!--        <el-form-item label="规格说明" prop="specifications">-->
          <!--          <el-input v-model="form.specifications" placeholder="请输入规格说明" />-->
          <!--        </el-form-item>-->
          <el-form-item label="库存" prop="stock">
            <el-input v-model="form.stock" placeholder="请输入库存" />
          </el-form-item>
        </el-card>
        <!-- 单规格编辑处-end -->
        <el-card v-if="isEducationGoods && form.educationExt" style="margin-bottom: 20px;">
          <div slot="header"><span>课程信息</span></div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="上课时间">
                <el-input v-model="form.educationExt.courseTime" placeholder="如：周一 09:00-10:30" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="授课地点">
                <el-input v-model="form.educationExt.coursePlace" placeholder="请输入授课地点" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="授课老师">
                <el-input v-model="form.educationExt.teacherName" placeholder="请输入授课老师" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="课次">
                <el-input-number v-model="form.educationExt.lessonCount" :min="1" controls-position="right" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="班级上限">
                <el-input-number v-model="form.educationExt.classSizeMax" :min="1" controls-position="right" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开班下限">
                <el-input-number v-model="form.educationExt.classSizeMin" :min="1" controls-position="right" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开课日期">
                <el-date-picker v-model="form.educationExt.startDate" type="date" value-format="yyyy-MM-dd" placeholder="选择开课日期" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="报名开始">
                <el-date-picker v-model="form.educationExt.signupStart" type="date" value-format="yyyy-MM-dd" placeholder="选择报名开始日期" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="报名截止">
                <el-date-picker v-model="form.educationExt.signupEnd" type="date" value-format="yyyy-MM-dd" placeholder="选择报名截止日期" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="咨询电话">
                <el-input v-model="form.educationExt.consultPhone" placeholder="请输入咨询电话" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="材料备注">
                <el-input v-model="form.educationExt.materialNote" type="textarea" placeholder="如：需自备水彩颜料、画笔等" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>
        <!--        <el-form-item label="是否置顶" prop="isTop">-->
        <!--          <el-radio-group v-model="form.isTop">-->
        <!--            <el-radio-->
        <!--              v-for="dict in dict.type.common_is_not"-->
        <!--              :key="dict.value"-->
        <!--              :label="parseInt(dict.value)"-->
        <!--            >{{dict.label}}</el-radio>-->
        <!--          </el-radio-group>-->
        <!--        </el-form-item>-->
        <el-form-item label="是否首页热门" prop="isHot">
          <el-radio-group v-model="form.isHot">
            <el-radio v-for="dict in dict.type.common_is_not" :key="dict.value"
                      :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <!--        <el-form-item label="分类属性id集合" prop="attrIds">-->
        <!--          <el-input v-model="form.attrIds" type="textarea" placeholder="请输入内容" />-->
        <!--        </el-form-item>-->
        <!--        <el-form-item label="分类属性值集合" prop="attrValues">-->
        <!--          <el-input v-model="form.attrValues" type="textarea" placeholder="请输入内容" />-->
        <!--        </el-form-item>-->
        <el-form-item label="推荐奖励类型" prop="awardType">
          <el-radio-group v-model="form.awardType">
            <el-radio v-for="dict in dict.type.award_type" :key="dict.value"
                      :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.awardType === '2'" label="父级奖励（%）" prop="awardParentRatio">
          <el-input v-model="form.awardParentRatio" placeholder="请输入父级奖励百分比" />
        </el-form-item>
        <el-form-item v-if="form.awardType === '2'" label="祖级奖励（%）" prop="awardGrandParentRatio">
          <el-input v-model="form.awardGrandParentRatio" placeholder="请输入祖级奖励百分比" />
        </el-form-item>
        <el-form-item v-if="form.awardType === '3'" label="父级奖励（元）" prop="awardParentRatio">
          <el-input v-model="form.awardParentRatio" placeholder="请输入父级奖励金额" />
        </el-form-item>
        <el-form-item v-if="form.awardType === '3'" label="祖级奖励（元）" prop="awardGrandParentRatio">
          <el-input v-model="form.awardGrandParentRatio" placeholder="请输入祖级奖励金额" />
        </el-form-item>
        <el-form-item label="奖励金币" prop="awardGolden">
          <el-input v-model="form.awardGolden" placeholder="请输入奖励金币" />
        </el-form-item>
        <el-form-item label="商品内容">
          <editor v-model="form.content" :min-height="192" />
        </el-form-item>
        <!-- 商品特色 -->
        <!-- 商品特色 -->
        <el-form-item label="商品特色" v-if="showFeatureSection">
          <div v-if="featureTabs.length === 0" style="margin-bottom: 15px;">
            <el-button size="small" @click="showAddFeatureDialog = true" icon="el-icon-plus">
              添加第一个商品特色
            </el-button>
          </div>

          <el-tabs v-else type="border-card" v-model="activeFeatureTab">
            <el-tab-pane
              v-for="feature in featureTabs"
              :key="feature.key"
              :label="feature.label"
              :name="feature.key">
              <div class="feature-header">
                <el-button
                  size="mini"
                  type="danger"
                  @click="removeFeatureTab(feature.key)"
                  icon="el-icon-delete">
                  删除
                </el-button>
              </div>
              <editor v-model="form.features[feature.key]" :min-height="192"/>
            </el-tab-pane>

            <div class="add-feature-tab">
              <el-button size="mini" @click="showAddFeatureDialog = true">
                <i class="el-icon-plus"></i> 添加特色
              </el-button>
            </div>
          </el-tabs>
        </el-form-item>
        <el-card class="box-card" style="margin-top: 20px;" v-if="form.isSku === 1 && !isEducationGoods">
          <div slot="header" class="clearfix">
            <span>规格管理</span>
            <el-button style="float: right;" type="primary" size="mini" @click="handleAddSku">
              添加规格
            </el-button>
          </div>

          <el-table
            :data="skuTreeData"
            border
            style="width: 100%"
            row-key="skuId"
            :tree-props="{children: 'children', hasChildren: 'hasChildren'}">
            <el-table-column prop="skuName" label="规格名称" width="150">
              <template slot-scope="scope">
                <span :style="{ paddingLeft: (scope.row.level || 0) * 20 + 'px' }">
                  {{ scope.row.skuName }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="skuType" label="类型" width="80">
              <template slot-scope="scope">
                <el-tag v-if="scope.row.skuType === '200'">标准</el-tag>
                <el-tag v-else-if="scope.row.skuType === '201'">自选</el-tag>
                <el-tag v-else type="success">组合</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="售价" width="80"></el-table-column>
            <el-table-column prop="stock" label="库存" width="80"></el-table-column>
            <el-table-column prop="stockUnit" label="库存单位" width="80"></el-table-column>
            <el-table-column prop="skuCode" label="编码" width="120"></el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template slot-scope="scope">
                <el-tag v-if="isSkuStatusEnabled(scope.row.status)" type="success">启用</el-tag>
                <el-tag v-else type="danger">停用</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template slot-scope="scope">
                <el-button size="mini" @click="handleEditSku(scope.row)">编辑</el-button>
                <el-button size="mini" @click="handleManageOptions(scope.row)">属性</el-button>
                <el-button size="mini" type="danger" @click="removeSku(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-form-item v-if="form.goodsType === 'online'" label="运费" prop="expressFee">
          <el-input v-model="form.expressFee" placeholder="请输入运费" />
        </el-form-item>
        <el-form-item v-if="form.goodsType === 'online'" label="重量(克)" prop="weight">
          <el-input v-model="form.weight" placeholder="请输入重量(克)" />
        </el-form-item>
        <el-form-item label="阅读次数" prop="viewCount">
          <el-input v-model="form.viewCount" placeholder="请输入阅读次数" />
        </el-form-item>
        <el-form-item label="销量" prop="saleCount">
          <el-input v-model="form.saleCount" placeholder="请输入销量" />
        </el-form-item>
        <el-form-item label="商品状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.goods_status" :key="dict.value"
                      :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    <el-dialog title="添加商品特色" :visible.sync="showAddFeatureDialog" width="400px">
      <!-- 修改表单绑定 -->
      <el-form ref="featureForm" :model="featureFormData" :rules="featureRules">
        <el-form-item label="选择特色" prop="selectedFeature">
          <el-select
            v-model="featureFormData.selectedFeature"
            placeholder="请选择商品特色"
            style="width: 100%"
            @change="handleFeatureSelect">
            <el-option
              v-for="item in featureOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="cancelFeature">取消</el-button>
        <el-button type="primary" @click="addFeatureTab">确定</el-button>
      </div>
    </el-dialog>
    <!-- SKU编辑对话框 -->
    <el-dialog :title="skuDialogTitle" :visible.sync="skuDialogVisible" width="600px">
      <el-form ref="skuForm" :model="currentSku" :rules="skuRules" label-width="100px">
        <el-form-item label="父级规格" prop="parSkuId">
          <el-select v-model="currentSku.parSkuId" placeholder="请选择父级规格" clearable>
            <el-option
              v-for="sku in parentSkuOptions"
              :key="sku.skuId"
              :label="sku.skuName"
              :value="sku.skuId">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="规格名称" prop="skuName">
          <el-input v-model="currentSku.skuName" placeholder="请输入规格名称"/>
        </el-form-item>
        <el-form-item label="规格类型" prop="skuType">
          <el-radio-group v-model="currentSku.skuType">
            <el-radio label="200">标准</el-radio>
            <el-radio label="201">自选</el-radio>
            <el-radio label="202">组合</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规格编码" prop="skuCode">
          <el-input v-model="currentSku.skuCode" placeholder="请输入规格编码"/>
        </el-form-item>
        <el-form-item label="售价" prop="price">
          <el-input v-model="currentSku.price" placeholder="请输入售价">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input v-model="currentSku.stock" placeholder="请输入库存数量"/>
        </el-form-item>
        <el-form-item label="库存单位" prop="stockUnit">
          <el-input v-model="currentSku.stockUnit" placeholder="请输入库存单位"/>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="currentSku.sortOrder" :min="1"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="currentSku.status"
            active-value="1"
            inactive-value="0"
            active-text="启用"
            inactive-text="停用">
          </el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="skuDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="addSkuToList">确 定</el-button>
      </div>
    </el-dialog>

    <!-- SKU属性选项管理对话框 -->
    <el-dialog title="规格属性管理" :visible.sync="optionDialogVisible" width="800px" :before-close="handleOptionDialogClose">
      <div slot="title">
        规格属性管理 - {{ currentSku.skuName }}
      </div>

      <el-button type="primary" size="mini" @click="handleAddOption" style="margin-bottom: 15px;">
        添加属性选项
      </el-button>

      <el-table :data="currentSku.options" border style="width: 100%">
        <el-table-column prop="optionName" label="属性名称" width="120"></el-table-column>
        <el-table-column prop="optionType" label="属性类型" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.optionType === '301'">单价</el-tag>
            <el-tag v-else-if="scope.row.optionType === '302'">总价</el-tag>
            <el-tag v-else-if="scope.row.optionType === '303'">数量</el-tag>
            <el-tag v-else-if="scope.row.optionType === '304'">文本</el-tag>
            <el-tag v-else-if="scope.row.optionType === '305'">图片</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="optionValue" label="选项值" width="100">
          <template slot-scope="scope">
            <!-- 当选项类型为图片(305)时显示图片预览 -->
            <div v-if="scope.row.optionType === '305'">
              <image-preview
                :src="scope.row.optionValue"
                :width="50"
                :height="50"
                v-if="scope.row.optionValue"
              />
              <span v-else>无图片</span>
            </div>
            <!-- 其他类型时显示文本值 -->
            <span v-else>{{ scope.row.optionValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="optionValueUnit" label="单位" width="80">
        </el-table-column>
        <el-table-column prop="optionSort" label="顺序号" width="80"></el-table-column>
        <el-table-column prop="skuSeqNo" label="规格编号" width="80"></el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button size="mini" @click="handleEditOption(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="removeOption(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer">
        <el-button @click="optionDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 属性选项编辑对话框 -->
    <el-dialog :title="optionDialogTitle" :visible.sync="optionEditDialogVisible" width="500px">
      <el-form ref="optionForm" :model="currentOption" :rules="optionRules" label-width="100px">
        <el-form-item label="选项名称" prop="optionName">
          <el-input v-model="currentOption.optionName" placeholder="请输入选项名称"/>
        </el-form-item>
        <el-form-item label="选项类型" prop="optionType">
          <el-select v-model="currentOption.optionType" placeholder="请选择选项类型">
            <el-option label="单价" value="301"></el-option>
            <el-option label="总价" value="302"></el-option>
            <el-option label="数量" value="303"></el-option>
            <el-option label="文本" value="304"></el-option>
            <el-option label="图片" value="305"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选项值" prop="optionValue">
          <div v-if="currentOption.optionType === '305'">
            <image-upload
              v-model="currentOption.optionValue"
              :limit="1"
            />
            <div class="tip-text">请上传图片文件</div>
          </div>
          <el-input
            v-else
            v-model="currentOption.optionValue"
            placeholder="请输入选项值"
          />
          <div
            v-if="currentOption.optionType === '301' || currentOption.optionType === '302' || currentOption.optionType === '303'"
            class="tip-text"
          >
            请输入数值
          </div>
          <div
            v-else-if="currentOption.optionType === '304'"
            class="tip-text"
          >
            请输入文本内容
          </div>
        </el-form-item>
        <el-form-item label="单位" prop="optionValueUnit">
          <el-input v-model="currentOption.optionValueUnit" placeholder="请输入单位"/>
        </el-form-item>
        <el-form-item label="顺序号" prop="optionSort">
          <el-input-number v-model="currentOption.optionSort" :min="1" placeholder="请输入顺序号"/>
        </el-form-item>
        <el-form-item label="规格编号" prop="skuSeqNo">
          <el-input-number v-model="currentOption.skuSeqNo" :min="0" placeholder="请输入规格编号"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="currentOption.status"
            active-value="1"
            inactive-value="0"
            active-text="启用"
            inactive-text="停用">
          </el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="optionEditDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="addOptionToCurrentSku">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApp_goods, getApp_goods, delApp_goods, addApp_goods, updateApp_goods } from "@/api/system/app_goods";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { listApp_goods_category } from "@/api/system/app_goods_category";
import { listDept } from "@/api/system/dept";
const hotelFeatureLabelMap = {
  'basic': '基本特色',
  'food': '餐饮',
  'traffic': '交通',
  'entertainment': '娱乐',
  'attractions': '周边景点',
  'medical': '医疗',
  'shopping': '购物',
  'policy': '政策'
};
const educationFeatureLabelMap = {
  'course_content': '课程内容',
  'signup_info': '报名信息',
  'signup_notice': '报名须知'
};
export default {
  name: "App_goods",
  dicts: ['common_is_not', 'award_type', 'goods_status', 'goods_type'],
  components: {
    Treeselect
  },
  data() {
    return {
      goodsId: null,
      goodsInfo: {},
      skuList: [],
      parentSkuOptions: [], // 确保初始化
      // SKU编辑对话框
      skuDialogVisible: false,
      skuDialogTitle: '',
      // 当前编辑的SKU
      currentSku: {
        skuId: null,
        goodsId: null,
        skuName: '',
        skuType: '201',
        skuCode: '',
        parSkuId: 0,
        price: '',        // 售价
        stock: '',        // 库存
        stockUnit: '',    // 库存单位
        sortOrder: 1,
        status: '1',
        validTime: null,
        invalidTime: null,
        options: [] // SKU属性选项，确保初始化为空数组
      },
      skuRules: {
        skuName: [{ required: true, message: '规格名称不能为空', trigger: 'blur' }],
        skuType: [{ required: true, message: '规格类型不能为空', trigger: 'change' }],
        price: [{ required: true, message: '售价不能为空', trigger: 'blur' }],
        stock: [{ required: true, message: '库存不能为空', trigger: 'blur' }],
        stockUnit: [{ required: true, message: '库存单位不能为空', trigger: 'blur' }]
      },
      // 属性选项管理对话框
      optionDialogVisible: false,
      // SKU属性选项
      skuOptions: [],

      // 属性选项编辑对话框
      optionEditDialogVisible: false,
      optionDialogTitle: '',
      currentOption: {
        optionId: null,
        goodsId: null,
        skuId: null,
        optionType: '',
        optionName: '',
        optionValue: '',
        optionValueUnit: '',  // 单位改为输入框
        optionSort: 1,         // 添加顺序号
        status: '1',
        skuSeqNo: 1
      },
      optionRules: {
        optionName: [{ required: true, message: '选项名称不能为空', trigger: 'blur' }],
        optionType: [{ required: true, message: '选项类型不能为空', trigger: 'change' }],
        skuSeqNo: [{ required: true, message: '规格编号不能为空', trigger: 'blur' }]
      },
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
      activeFeatureTab: '',
      // 总条数
      total: 0,
      // 商品表格数据
      app_goodsList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 将 newFeature 改为下拉选择模式
      selectedFeature: '',// 初始值应为空字符串
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'goodsId',
        isAsc: 'descending',
        categoryId: null,
        deptId: null,
        goodsName: null,
        tags: null,
        stock: null,
        goodsType: null,
        isTop: null,
        isHot: null,
        isSku: null,
        awardType: null,
        status: null
      },
      searchKeyword: '',
      showAdvancedSearch: false,
      defaultSort: { prop: 'goodsId', order: 'descending' },
      debouncedQuery: null,
      // 表单参数
      form: {
        features: {}
      },
      // 特色标签页配置
      featureTabs: {},
      showAddFeatureDialog: false,
      newFeature: {
        name: '',
        key: ''
      },
      featureFormData: {
        selectedFeature: ''
      },
      featureRules: {
        selectedFeature: [{ required: true, message: '请选择商品特色', trigger: 'change' }]
      },
      // 表单校验
      rules: {
        categoryId: [
          { required: true, message: "所属分类不能为空", trigger: "change" }
        ],
        deptId: [
          { required: true, message: "所属分站不能为空", trigger: "blur" }
        ],
        goodsName: [
          { required: true, message: "商品名称不能为空", trigger: "blur" }
        ],
      },

      // 分站树选项
      deptOptions: [],
      // 商品分类树选项
      app_goods_categoryOptions: [],
    };
  },
  computed: {
    isEducationGoods() {
      return this.form.goodsType === 'education';
    },
    showFeatureSection() {
      return this.form.isSku === 1 || this.isEducationGoods;
    },
    featureOptions() {
      const labelMap = this.isEducationGoods ? educationFeatureLabelMap : hotelFeatureLabelMap;
      return Object.keys(labelMap).map(key => ({
        value: key,
        label: labelMap[key]
      }));
    },
    skuTreeData() {
      return this.buildSkuTree(this.skuList);
    },
    activeFilterTags() {
      const tags = []
      if (this.searchKeyword) {
        tags.push({ key: 'keyword', label: `关键词：${this.searchKeyword}` })
      }
      if (this.queryParams.goodsType) {
        const found = (this.dict.type.goods_type || []).find(item => item.value === this.queryParams.goodsType)
        tags.push({ key: 'goodsType', label: `类型：${found ? found.label : this.queryParams.goodsType}` })
      }
      if (this.queryParams.status) {
        const found = (this.dict.type.goods_status || []).find(item => item.value === this.queryParams.status)
        tags.push({ key: 'status', label: `状态：${found ? found.label : this.queryParams.status}` })
      }
      if (this.queryParams.isHot !== null && this.queryParams.isHot !== '') {
        const found = (this.dict.type.common_is_not || []).find(item => item.value == this.queryParams.isHot)
        tags.push({ key: 'isHot', label: `热推：${found ? found.label : this.queryParams.isHot}` })
      }
      if (this.queryParams.deptId) {
        tags.push({ key: 'deptId', label: `分站：${this.findTreeLabel(this.deptOptions, this.queryParams.deptId) || this.queryParams.deptId}` })
      }
      if (this.queryParams.categoryId) {
        tags.push({ key: 'categoryId', label: `分类：${this.findTreeLabel(this.app_goods_categoryOptions, this.queryParams.categoryId) || this.queryParams.categoryId}` })
      }
      if (this.queryParams.goodsName) {
        tags.push({ key: 'goodsName', label: `名称：${this.queryParams.goodsName}` })
      }
      if (this.queryParams.tags) {
        tags.push({ key: 'tags', label: `标签：${this.queryParams.tags}` })
      }
      return tags
    }
  },
  watch: {
    app_goodsList() {
      this.syncTableLayout()
    },
    'form.goodsType'(val, oldVal) {
      if (val === 'education') {
        this.form.isSku = 0;
        if (!this.form.educationExt) {
          this.$set(this.form, 'educationExt', this.createDefaultEducationExt());
        }
        if (!this.form.unit) {
          this.form.unit = '期';
        }
      }
      if (oldVal && val !== oldVal && (val === 'education' || oldVal === 'education')) {
        this.featureTabs = [];
        this.form.features = {};
        this.activeFeatureTab = '';
      }
    }
  },
  created() {
    this.debouncedQuery = this.debounce(this.handleQuery, 400)
    this.getDeptTreeselect();
    this.getCategoryTreeselect();
    this.getList();
  },
  mounted() {
    this.syncTableLayout()
    this._resizeHandler = () => this.syncTableLayout()
    window.addEventListener('resize', this._resizeHandler)
  },
  beforeDestroy() {
    if (this._resizeHandler) {
      window.removeEventListener('resize', this._resizeHandler)
    }
  },
  methods: {
    createDefaultEducationExt() {
      return {
        courseTime: '',
        coursePlace: '',
        teacherName: '',
        lessonCount: null,
        classSizeMax: null,
        classSizeMin: null,
        startDate: null,
        signupStart: null,
        signupEnd: null,
        materialNote: '',
        consultPhone: '13764363947'
      };
    },
    debounce(fn, delay) {
      let timer = null
      return function(...args) {
        clearTimeout(timer)
        timer = setTimeout(() => fn.apply(this, args), delay)
      }
    },
    buildListQueryParams() {
      const params = { ...this.queryParams }
      if (this.searchKeyword && this.searchKeyword.trim()) {
        params.params = { ...(params.params || {}), keyword: this.searchKeyword.trim() }
      }
      return params
    },
    findTreeLabel(nodes, id) {
      if (!nodes || id === null || id === undefined) return ''
      for (const node of nodes) {
        if (node.id === id) return node.label
        if (node.children && node.children.length) {
          const found = this.findTreeLabel(node.children, id)
          if (found) return found
        }
      }
      return ''
    },
    syncTableLayout() {
      this.$nextTick(() => {
        if (this.$refs.goodsTable) {
          this.$refs.goodsTable.doLayout()
        }
      })
    },
    clearFilterTag(key) {
      switch (key) {
        case 'keyword':
          this.searchKeyword = ''
          break
        case 'goodsType':
          this.queryParams.goodsType = null
          break
        case 'status':
          this.queryParams.status = null
          break
        case 'isHot':
          this.queryParams.isHot = null
          break
        case 'deptId':
          this.queryParams.deptId = null
          break
        case 'categoryId':
          this.queryParams.categoryId = null
          break
        case 'goodsName':
          this.queryParams.goodsName = null
          break
        case 'tags':
          this.queryParams.tags = null
          break
        default:
          break
      }
      this.handleQuery()
    },
    // 构建父级SKU选项
    buildParentSkuOptions() {
      // 只选择根级SKU（parSkuId为0）和当前编辑的SKU除外的SKU作为父级选项
      console.log('=== buildParentSkuOptions 开始 ===');
      console.log('skuList:', JSON.parse(JSON.stringify(this.skuList)));
      console.log('currentSku.skuId:', this.currentSku.skuId);

      this.parentSkuOptions = this.skuList.filter(sku => {
        const isRoot = sku.parSkuId === 0 || sku.parSkuId === '0' || !sku.parSkuId;
        const isNotSelf = !this.currentSku.skuId || sku.skuId !== this.currentSku.skuId;

        console.log(`SKU: ${sku.skuName}, parSkuId: ${sku.parSkuId}, isRoot: ${isRoot}, isNotSelf: ${isNotSelf}`);

        return isRoot && isNotSelf;
      });

      console.log('过滤后的 parentSkuOptions:', JSON.parse(JSON.stringify(this.parentSkuOptions)));
      console.log('=== buildParentSkuOptions 结束 ===');
    },
    // 构建SKU树状结构
    buildSkuTree(skuList) {
      const map = {};
      const roots = [];

      // 初始化所有节点
      skuList.forEach(sku => {
        map[sku.skuId] = { ...sku, children: [] };
      });

      // 构建父子关系
      skuList.forEach(sku => {
        const node = map[sku.skuId];
        if (sku.parSkuId === 0) {
          roots.push(node);
        } else {
          const parent = map[sku.parSkuId];
          if (parent) {
            parent.children.push(node);
          } else {
            roots.push(node);
          }
        }
      });

      return roots;
    },
    handleOptionDialogClose(done) {
      // 只有在确实有修改时才更新数据
      const index = this.skuList.findIndex(item => item.skuId === this.currentSku.skuId);
      if (index !== -1) {
        // 确保深拷贝选项数据
        this.$set(this.skuList[index], 'options', [...this.currentSku.options]);
      }
      done();
    },
    // 添加SKU到列表
    addSkuToList() {
      this.$refs['skuForm'].validate(valid => {
        if (valid) {
          if (this.currentSku.skuId) {
            // 编辑模式 - 更新现有SKU
            const index = this.skuList.findIndex(item => item.skuId === this.currentSku.skuId);
            if (index !== -1) {
              this.$set(this.skuList, index, {...this.currentSku});
            }
          } else {
            // 新增模式 - 添加新SKU
            this.currentSku.skuId = 'temp_' + Date.now(); // 临时ID
            this.skuList.push({...this.currentSku});
          }
          this.skuDialogVisible = false;
        }
      });
    },
    // 添加属性选项到当前SKU
    addOptionToCurrentSku() {
      this.$refs['optionForm'].validate(valid => {
        if (valid) {
          // 确保 currentSku.options 存在且为数组
          if (!this.currentSku.options) {
            this.$set(this.currentSku, 'options', []);
          }

          if (this.currentOption.optionId && this.currentOption.optionId.toString().startsWith('temp_')) {
            // 编辑现有选项
            const index = this.currentSku.options.findIndex(
              item => item.optionId === this.currentOption.optionId
            );
            if (index !== -1) {
              this.$set(this.currentSku.options, index, {...this.currentOption});
            }
          } else if (!this.currentOption.optionId) {
            // 新增选项
            this.currentOption.optionId = 'temp_' + Date.now();
            this.currentSku.options.push({...this.currentOption});
          } else {
            // 编辑已有选项
            const index = this.currentSku.options.findIndex(
              item => item.optionId === this.currentOption.optionId
            );
            if (index !== -1) {
              this.$set(this.currentSku.options, index, {...this.currentOption});
            }
          }
          this.optionEditDialogVisible = false;
        }
      });
    },
    // 获取SKU列表
    getSkuList() {
      /*listSku(this.goodsId).then(response => {
        this.skuList = response.data;
        // 构建父SKU选项
        this.parentSkuOptions = this.skuList.filter(item => item.parSkuId === 0);
      });*/
    },
    // 添加SKU
    handleAddSku() {
      this.skuDialogTitle = '添加规格';
      this.currentSku = {
        skuId: null,
        goodsId: this.goodsId,
        skuName: '',
        skuType: '201',
        skuCode: '',
        parSkuId: 0,
        sortOrder: 0,
        status: '1',
        validTime: null,
        invalidTime: null,
        options: []
      };
      this.buildParentSkuOptions(); // 添加这行
      this.skuDialogVisible = true;
    },
    // 编辑SKU时加载属性选项
    handleEditSku(row) {
      this.skuDialogTitle = '编辑规格';
      // 从 skuList 中查找完整且最新的SKU数据
      const existingSku = this.skuList.find(item => item.skuId === row.skuId);
      if (existingSku) {
        // 使用 skuList 中的完整数据
        this.currentSku = this.normalizeSkuStatus({ ...existingSku });
      } else {
        // 如果在 skuList 中找不到，则使用传入的 row 数据
        this.currentSku = this.normalizeSkuStatus({
          ...row,
          options: row.options || []
        });
      }
      this.buildParentSkuOptions();
      this.skuDialogVisible = true;
    },
    /** 规格状态统一为字符串 '1'/'0'，避免开关与展示因类型不一致一直显示停用 */
    isSkuStatusEnabled(status) {
      return status === 1 || status === '1' || status === true || status === '启用';
    },
    normalizeSkuStatus(sku) {
      if (!sku) return sku;
      const enabled = this.isSkuStatusEnabled(sku.status);
      return { ...sku, status: enabled ? '1' : '0' };
    },
    // 删除SKU
    removeSku(row) {
      this.$modal.confirm('确认要删除规格 "' + row.skuName + '" 吗？').then(() => {
        const index = this.skuList.findIndex(item => item.skuId === row.skuId);
        if (index !== -1) {
          this.skuList.splice(index, 1);
        }
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    // 删除SKU
    handleDeleteSku(row) {
      this.$modal.confirm('确认要删除规格 "' + row.skuName + '" 吗？').then(() => {
        //return delSku(row.skuId);
      }).then(() => {
        this.getSkuList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    // 处理特色选择
    handleFeatureSelect(value) {
      // 如果选择的是预定义特色，自动填充名称和标识符
      //const selectedOption = this.featureOptions.find(option => option.value === value);
      this.featureFormData.selectedFeature = value;
    },
    // 提交SKU表单
    submitSkuForm() {
      this.$refs['skuForm'].validate(valid => {
        if (valid) {
          if (this.currentSku.skuId) {
            /*updateSku(this.currentSku).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.skuDialogVisible = false;
              this.getSkuList();
            });*/
          } else {
            /*addSku(this.currentSku).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.skuDialogVisible = false;
              this.getSkuList();
            });*/
          }
        }
      });
    },
    // 管理SKU属性选项
    handleManageOptions(row) {
      // 从 skuList 中查找完整且最新的SKU数据
      const existingSku = this.skuList.find(item => item.skuId === row.skuId);
      if (existingSku) {
        // 使用 skuList 中的完整数据
        this.currentSku = { ...existingSku };
      } else {
        // 如果在 skuList 中找不到，则使用传入的 row 数据
        this.currentSku = {
          ...row,
          options: row.options || []
        };
      }
      this.optionDialogVisible = true;
    },
    // 获取SKU属性选项
    getSkuOptions(skuId) {
      /*listSkuOption(skuId).then(response => {
        this.skuOptions = response.data;
      });*/
    },
    // 添加属性选项
    handleAddOption() {
      this.optionDialogTitle = '添加属性选项';
      this.currentOption = {
        optionId: null,
        goodsId: this.goodsId,
        skuId: this.currentSku.skuId,
        optionType: '',
        optionName: '',
        optionValue: '',
        optionValueUnit: '',
        optionSort: 1,
        status: '1',
        skuSeqNo: 1
      };
      this.optionEditDialogVisible = true;
    },
    // 编辑属性选项
    handleEditOption(row) {
      this.optionDialogTitle = '编辑属性选项';
      this.currentOption = { ...row };
      this.optionEditDialogVisible = true;
    },
    // 删除属性选项
    removeOption(row) {
      this.$modal.confirm('确认要删除属性选项 "' + row.optionName + '" 吗？').then(() => {
        const index = this.currentSku.options.findIndex(item => item.optionId === row.optionId);
        if (index !== -1) {
          this.currentSku.options.splice(index, 1);
        }
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    // 删除属性选项
    handleDeleteOption(row) {
      this.$modal.confirm('确认要删除属性选项 "' + row.optionName + '" 吗？').then(() => {
        //return delSkuOption(row.optionId);
      }).then(() => {
        //this.getSkuOptions(this.currentSku.skuId);
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    // 提交属性选项表单
    submitOptionForm() {
      this.$refs['optionForm'].validate(valid => {
        if (valid) {
          if (this.currentOption.optionId) {
            /*updateSkuOption(this.currentOption).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.optionEditDialogVisible = false;
              this.getSkuOptions(this.currentSku.skuId);
            });*/
          } else {
            /*addSkuOption(this.currentOption).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.optionEditDialogVisible = false;
              this.getSkuOptions(this.currentSku.skuId);
            });*/
          }
        }
      });
    },
    // 添加特色标签页
    addFeatureTab() {
      if (!this.$refs.featureForm) {
        this.$message.error('表单引用不存在');
        return;
      }

      this.$refs.featureForm.validate((valid) => {
        if (valid) {
          // 使用 featureFormData.selectedFeature 替代 selectedFeature
          const key = this.featureFormData.selectedFeature;
          const selectedOption = this.featureOptions.find(option => option.value === key);
          const name = selectedOption ? selectedOption.label : key;

          // 检查是否已存在
          const exists = this.featureTabs.some(tab => tab.key === key);
          if (exists) {
            this.$message.warning('该特色已存在');
            return;
          }

          // 添加新标签页
          this.featureTabs.push({
            key: key,
            label: name,
            custom: true
          });

          // 设置为当前激活标签
          this.activeFeatureTab = key;

          // 初始化内容（动态添加到 form.features）
          if (!this.form.features) {
            this.$set(this.form, 'features', {});
          }
          if (!this.form.features[key]) {
            this.$set(this.form.features, key, '');
          }

          // 关闭对话框并重置表单
          this.showAddFeatureDialog = false;
          this.featureFormData.selectedFeature = '';
        } else {
          this.$message.warning('请选择商品特色');
        }
      });
    },

// 删除特色标签页
    removeFeatureTab(key) {
      this.$confirm(`确定要删除"${this.getFeatureLabel(key)}"特色吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 找到要删除的标签索引
        const index = this.featureTabs.findIndex(tab => tab.key === key);
        if (index !== -1) {
          // 删除标签配置
          this.featureTabs.splice(index, 1);
          // 删除对应数据
          this.$delete(this.form.features, key);

          // 如果删除的是当前激活的tab，切换到第一个tab或清空
          if (this.featureTabs.length > 0 && this.activeFeatureTab === key) {
            this.activeFeatureTab = this.featureTabs[0].key;
          } else if (this.featureTabs.length === 0) {
            this.activeFeatureTab = '';
          }
        }

        this.$message.success('删除成功');
      }).catch(() => {});
    },

// 获取特色标签的显示名称
    getFeatureLabel(key) {
      const feature = this.featureTabs.find(tab => tab.key === key);
      return feature ? feature.label : key;
    },
    /** 查询商品列表 */
    getList() {
      this.loading = true;
      listApp_goods(this.buildListQueryParams()).then(response => {
        this.app_goodsList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.syncTableLayout();
      });
    },
    /** 转换分站数据结构 */
    normalizerDept(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.deptId,
        label: node.deptName,
        children: node.children
      };
    },
    /** 转换商品分类数据结构 */
    normalizerCategory(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.categoryId,
        label: node.categoryName,
        children: node.children
      };
    },
    /** 查询部门列表 */
    getDeptList() {
      // this.loading = true;
      listDept(this.queryParams).then(response => {
        this.deptList = this.handleTree(response.data, "deptId");
        // this.loading = false;
      });
    },
    /** 查询部门下拉树结构 */
    getDeptTreeselect() {
      listDept().then(response => {
        this.deptOptions = [];
        const data = { deptId: 0, deptName: '无站点', children: [] };
        data.children = this.handleTree(response.data, "deptId", "parentId");
        this.deptOptions.push(data);
      });
    },
    /** 查询商品分类下拉树结构 */
    getCategoryTreeselect() {
      listApp_goods_category().then(response => {
        this.app_goods_categoryOptions = [];
        const data = { categoryId: 0, categoryName: '无分类', children: [] };
        data.children = this.handleTree(response.data, "categoryId", "parentId");
        this.app_goods_categoryOptions.push(data);
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    cancelFeature(){
      this.showAddFeatureDialog = false;
      this.featureFormData.selectedFeature = ''; // 重置表单数据
    },
    // 表单重置
    reset() {
      this.form = {
        goodsId: null,
        categoryId: null,
        categoryIds: null,
        deptId: null,
        goodsName: null,
        goodsCover: null,
        goodsImages: null,
        description: null,
        tags: null,
        price: null,
        unit: null,
        specifications: null,
        stock: null,
        goodsType: null,
        isTop: null,
        isHot: null,
        attrIds: null,
        attrValues: null,
        isSku: null,
        awardType: null,
        awardParentRatio: null,
        awardGrandParentRatio: null,
        content: null,
        expressFee: null,
        weight: null,
        viewCount: null,
        saleCount: null,
        createTime: null,
        updateTime: null,
        status: null,
        features: {},  // 保持空对象，用于动态添加
        educationExt: null
      };
      this.featureTabs = []; // 清空特色标签
      this.activeFeatureTab = ''; // 重置激活的标签页
      this.skuList = [];
      this.currentSku = {
        skuId: null,
        goodsId: null,
        skuName: '',
        skuType: '201',
        skuCode: '',
        parSkuId: 0,
        sortOrder: 0,
        status: '1',
        validTime: null,
        invalidTime: null,
        options: []
      };
      this.skuOptions = [];
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 排序触发事件 */
    handleSortChange(column) {
      if (!column.prop || !column.order) {
        this.queryParams.orderByColumn = 'goodsId';
        this.queryParams.isAsc = 'descending';
        this.defaultSort = { prop: 'goodsId', order: 'descending' };
      } else {
        this.queryParams.orderByColumn = column.prop;
        this.queryParams.isAsc = column.order;
        this.defaultSort = { prop: column.prop, order: column.order };
      }
      this.handleQuery();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.searchKeyword = '';
      this.showAdvancedSearch = false;
      this.queryParams.orderByColumn = 'goodsId';
      this.queryParams.isAsc = 'descending';
      this.defaultSort = { prop: 'goodsId', order: 'descending' };
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.goodsId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.getCategoryTreeselect();
      this.getDeptTreeselect();
      this.open = true;
      this.title = "添加商品";
      // 初始化默认激活标签为空
      this.activeFeatureTab = '';
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getCategoryTreeselect();
      this.getDeptTreeselect();
      const goodsId = row.goodsId || this.ids
      getApp_goods(goodsId).then(response => {
        this.form = response.data;
        if (this.form.goodsType === 'education' && !this.form.educationExt) {
          this.$set(this.form, 'educationExt', this.createDefaultEducationExt());
        }
        this.open = true;
        this.title = "修改商品";
        // 加载SKU数据（如果存在）
        if (response.data.optionList) {
          this.skuList = response.data.optionList.map(sku => this.normalizeSkuStatus(sku));
        }
        // 确保 features 对象存在
        if (!this.form.features) {
          this.form.features = {};
        }
        /*if (!this.form.features) {
          Object.keys(this.form.features).forEach(key => {
            this.featureTabs.push({
              key: key,
              label: this.formatLabel(key),
              custom: true
            });
          });
        }
        // 设置默认激活标签
        if (this.featureTabs.length > 0 && !this.activeFeatureTab) {
          this.activeFeatureTab = this.featureTabs[0].key;
        }*/
        /*if (this.form.featureTabs) {
          this.featureTabs = this.form.featureTabs;
        } else if (this.form.features) {
          Object.keys(this.form.features).forEach(key => {
            this.featureTabs.push({
              key: key,
              label: this.formatLabel(key),
              custom: true
            });
          });
        }
        // 设置默认激活标签
        if (this.featureTabs.length > 0 && !this.activeFeatureTab) {
          this.activeFeatureTab = this.featureTabs[0].key;
        }*/
        // 处理商品特色数据
        // 如果后端返回的是 featureList 数组格式，需要转换为 features 对象和 featureTabs 数组
        const featureList = response.data.features;
        if (featureList && Array.isArray(featureList)) {
          // 清空现有数据
          this.featureTabs = [];
          this.form.features = {};

          // 转换数据结构
          featureList.forEach((item, index) => {
            const key = item.sectionId;
            const label = item.sectionName;

            // 构建 featureTabs
            this.featureTabs.push({
              key: key,
              label: label,
              custom: true
            });

            // 构建 features 对象
            this.$set(this.form.features, key, item.content || '');
          });
        } else if (this.form.features && typeof this.form.features === 'object') {
          // 如果已经是 features 对象格式，直接构建 featureTabs
          this.featureTabs = [];
          Object.keys(this.form.features).forEach(key => {
            this.featureTabs.push({
              key: key,
              label: this.formatLabel(key),
              custom: true
            });
          });
        }

        // 设置默认激活标签
        if (this.featureTabs.length > 0 && !this.activeFeatureTab) {
          this.activeFeatureTab = this.featureTabs[0].key;
        }
      });
    },
    // 格式化标签显示名称
    formatLabel(key) {
      const map = this.isEducationGoods ? educationFeatureLabelMap : hotelFeatureLabelMap;
      return map[key] || key.charAt(0).toUpperCase() + key.slice(1);
    },
    /** 属性配置 */
    handleSku(row) {
      // this.reset();
      // const goodsId = row.goodsId
      // this.$router.push({path: "/goods/app_goods_sku_data", query: {goodsId: goodsId}});
      this.reset();
      const goodsId = row.goodsId
      this.$router.push({ path: "/goods/app_goods_sku_data", query: { goodsId: goodsId } });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 整合所有数据（商品信息、SKU列表、商品特色）
          // 轁换 features 数据结构
          const featureList = [];
          if (this.form.features && typeof this.form.features === 'object') {
            Object.keys(this.form.features).forEach((key, index) => {
              const featureTab = this.featureTabs.find(tab => tab.key === key);
              if (featureTab) {
                featureList.push({
                  sectionId: key,
                  sectionName: featureTab.label,
                  content: this.form.features[key],
                  sortOrder: index,
                  minContentLength: 250 // 默认值
                });
              }
            });
          }
          // 添加调试日志
          console.log('SKU List:', this.skuList);
          this.skuList.forEach((sku, index) => {
            console.log(`SKU ${index}:`, sku);
            console.log(`SKU ${index} options:`, sku.options);
          });
          // 处理SKU列表，将临时ID替换为null
          const processedSkuList = this.skuList.map(sku => {
            // 确保options字段存在且为数组

            // 处理临时ID
            let thetmpSkuId = ''+sku.skuId;
            let thetmpParSkuId = ''+sku.parSkuId;
            let processedSkuId = sku.skuId;
            let processedParSkuId = sku.parSkuId;
            if (processedSkuId && processedSkuId.toString().startsWith('temp_')) {
              processedSkuId = null;
            }
            if (processedParSkuId && processedParSkuId.toString().startsWith('temp_')) {
              processedParSkuId = null;
            }

            let processedOptions = [];
            if (Array.isArray(sku.options)) {
              processedOptions = sku.options.map(option => {
                // 如果optionId以'temp_'开头，则置为空
                if (option.optionId && option.optionId.toString().startsWith('temp_')) {
                  return {
                    ...option,
                    skuId: processedSkuId,
                    optionId: null // 将临时optionId置为空
                  };
                }
                return option;
              });
            }

            return {
              ...sku,
              skuId: processedSkuId,
              parSkuId: processedParSkuId,
              tmpSkuId: thetmpSkuId,
              tmpParSkuId: thetmpParSkuId,
              options: processedOptions
            };
          });
          const formData = {
            ...this.form,
            optionList: this.isEducationGoods ? [] : processedSkuList,
            features: featureList
          };
          if (this.isEducationGoods && !formData.educationExt) {
            formData.educationExt = this.createDefaultEducationExt();
          }
          if (this.form.goodsId != null) {
            updateApp_goods(formData).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods(formData).then(response => {
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
      const goodsIds = row.goodsId || this.ids;
      this.$modal.confirm('是否确认删除商品编号为"' + goodsIds + '"的数据项？').then(function () {
        return delApp_goods(goodsIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => { });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods/export', {
        ...this.buildListQueryParams()
      }, `app_goods_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
<style scoped>
.filter-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 16px 4px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
}

.goods-search-form {
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

.goods-page {
  ::v-deep .el-table {
    .cell {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      line-height: 23px;
    }

    .cell > div {
      display: inline-block;
      max-width: 100%;
      vertical-align: middle;
      line-height: 23px;
    }

    .el-tag {
      vertical-align: middle;
    }
  }
}

.add-feature-tab {
  position: absolute;
  right: 100px;
  top: 15px;
}

.feature-header {
  text-align: right;
  margin-bottom: 10px;
}
.tip-text {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}
</style>
