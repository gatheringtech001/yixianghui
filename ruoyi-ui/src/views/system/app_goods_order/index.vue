<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="110px">
      <el-form-item label="用户ID" prop="userId" v-if="false">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品ID" prop="goodsId" v-if="false">
        <el-input
          v-model="queryParams.goodsId"
          placeholder="请输入商品ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属分站" prop="deptId">
        <el-input
          v-model="queryParams.deptId"
          placeholder="请输入所属分站"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入订单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="商品合计金额" prop="moneyTotal">-->
<!--        <el-input-->
<!--          v-model="queryParams.moneyTotal"-->
<!--          placeholder="请输入商品合计金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="折扣金额" prop="moneyDiscount">-->
<!--        <el-input-->
<!--          v-model="queryParams.moneyDiscount"-->
<!--          placeholder="请输入折扣金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="商品应付金额" prop="moneyPayable">-->
<!--        <el-input-->
<!--          v-model="queryParams.moneyPayable"-->
<!--          placeholder="请输入商品应付金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="快递费" prop="moneyExpress">-->
<!--        <el-input-->
<!--          v-model="queryParams.moneyExpress"-->
<!--          placeholder="请输入快递费"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="是否已支付" prop="payStatus">
        <el-select v-model="queryParams.payStatus" placeholder="请选择是否已支付" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
<!--      <el-form-item label="支付金额" prop="payMoney">-->
<!--        <el-input-->
<!--          v-model="queryParams.payMoney"-->
<!--          placeholder="请输入支付金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="支付方式" prop="payType">-->
<!--        <el-select v-model="queryParams.payType" placeholder="请选择支付方式" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.pay_type"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="支付时间" prop="payTime">-->
<!--        <el-date-picker clearable-->
<!--          v-model="queryParams.payTime"-->
<!--          type="date"-->
<!--          value-format="yyyy-MM-dd"-->
<!--          placeholder="请选择支付时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="商品总数量" prop="goodsCount">-->
<!--        <el-input-->
<!--          v-model="queryParams.goodsCount"-->
<!--          placeholder="请输入商品总数量"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="使用优惠券的id集合，逗号分割" prop="couponGotIds">-->
<!--        <el-input-->
<!--          v-model="queryParams.couponGotIds"-->
<!--          placeholder="请输入使用优惠券的id集合，逗号分割"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="发货时间" prop="sendTime">-->
<!--        <el-date-picker clearable-->
<!--          v-model="queryParams.sendTime"-->
<!--          type="date"-->
<!--          value-format="yyyy-MM-dd"-->
<!--          placeholder="请选择发货时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="发货快递名称" prop="sendExpressName">-->
<!--        <el-input-->
<!--          v-model="queryParams.sendExpressName"-->
<!--          placeholder="请输入发货快递名称"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="发货快递标识" prop="sendExpressSimple">-->
<!--        <el-input-->
<!--          v-model="queryParams.sendExpressSimple"-->
<!--          placeholder="请输入发货快递标识"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="发货快递单号" prop="sendExpressNo">-->
<!--        <el-input-->
<!--          v-model="queryParams.sendExpressNo"-->
<!--          placeholder="请输入发货快递单号"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="核销码" prop="checkNum">-->
<!--        <el-input-->
<!--          v-model="queryParams.checkNum"-->
<!--          placeholder="请输入核销码"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="是否已核销" prop="isChecked">-->
<!--        <el-select v-model="queryParams.isChecked" placeholder="请选择是否已核销" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.common_is_not"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="收货时间" prop="receiveTime">-->
<!--        <el-date-picker clearable-->
<!--          v-model="queryParams.receiveTime"-->
<!--          type="date"-->
<!--          value-format="yyyy-MM-dd"-->
<!--          placeholder="请选择收货时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="是否已经发放奖励" prop="isAward">-->
<!--        <el-select v-model="queryParams.isAward" placeholder="请选择是否已经发放奖励" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.common_is_not"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
      <el-form-item label="是否已评论" prop="isComment">
        <el-select v-model="queryParams.isComment" placeholder="请选择是否已评论" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否申请取消" prop="isApplyCancel">
        <el-select v-model="queryParams.isApplyCancel" placeholder="请选择是否申请取消" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="商户是否已结算" prop="isCash">
        <el-select v-model="queryParams.isCash" placeholder="请选择商户是否已结算" clearable>
          <el-option
            v-for="dict in dict.type.common_is_not"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择订单状态" clearable>
          <el-option
            v-for="dict in dict.type.order_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="旅居状态" prop="travelStatus">
        <el-select v-model="queryParams.travelStatus" placeholder="请选择旅居状态" clearable>
          <el-option
            v-for="item in travelStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:app_goods_order:add']"
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
          v-hasPermi="['system:app_goods_order:edit']"
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
          v-hasPermi="['system:app_goods_order:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:app_goods_order:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="app_goods_orderList"
              @selection-change="handleSelectionChange"
              style="width: 100%;min-width: 400px" max-height="400" :fit="true" v-el-table-column-resize>
      <el-table-column type="selection" width="55" align="center" fixed="left"/>
      <el-table-column label="订单ID" align="center" prop="orderId" v-if="false"/>
      <el-table-column label="用户ID" align="center" prop="userId" v-if="false"/>
      <el-table-column label="商品ID" align="center" prop="goodsId" v-if="false"/>
      <el-table-column label="所属分站" align="center" prop="deptId" v-if="false"/>
      <el-table-column label="所属分站" align="center" prop="deptName" fixed="left" min-width="120" show-overflow-tooltip/>
      <el-table-column label="收货地址" align="center" prop="addressId" v-if="false"/>
      <el-table-column label="订单号" align="center" prop="orderNo" fixed="left" min-width="180" :resizable="true" show-overflow-tooltip/>
      <el-table-column label="商品合计金额" align="center" prop="moneyTotal" fixed="left" min-width="80" :resizable="true"/>
      <el-table-column label="折扣金额" align="center" prop="moneyDiscount" min-width="80"/>
      <el-table-column label="商品应付金额" align="center" prop="moneyPayable" min-width="80" :resizable="true"/>
      <el-table-column label="快递费" align="center" prop="moneyExpress" min-width="80"/>
      <el-table-column label="是否已支付" align="center" prop="payStatus" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.payStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="支付金额" align="center" prop="payMoney" min-width="80"/>
      <el-table-column label="支付方式" align="center" prop="payType" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.pay_type" :value="scope.row.payType"/>
        </template>
      </el-table-column>
      <el-table-column label="支付时间" align="center" prop="payTime" min-width="100">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.payTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="商品总数量" align="center" prop="goodsCount" min-width="80"/>
      <el-table-column label="使用优惠券的id集合" align="center" prop="couponGotIds" min-width="80" show-overflow-tooltip/>
      <el-table-column label="订单备注" align="center" prop="remark" min-width="80" show-overflow-tooltip/>
      <el-table-column label="发货时间" align="center" prop="sendTime" min-width="80">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.sendTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发货快递名称" align="center" prop="sendExpressName" min-width="80" show-overflow-tooltip/>
      <el-table-column label="发货快递标识" align="center" prop="sendExpressSimple" min-width="80" show-overflow-tooltip/>
      <el-table-column label="发货快递单号" align="center" prop="sendExpressNo" min-width="80" show-overflow-tooltip/>
      <el-table-column label="核销码" align="center" prop="checkNum" min-width="80" show-overflow-tooltip/>
      <el-table-column label="是否已核销" align="center" prop="isChecked" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isChecked"/>
        </template>
      </el-table-column>
      <el-table-column label="收货时间" align="center" prop="receiveTime" min-width="80">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.receiveTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否已经发放奖励" align="center" prop="isAward" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isAward"/>
        </template>
      </el-table-column>
      <el-table-column label="是否已评论" align="center" prop="isComment" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isComment"/>
        </template>
      </el-table-column>
      <el-table-column label="是否申请取消" align="center" prop="isApplyCancel" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isApplyCancel"/>
        </template>
      </el-table-column>
      <el-table-column label="商户是否已结算" align="center" prop="isCash" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.common_is_not" :value="scope.row.isCash"/>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" prop="status" min-width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="旅居状态" align="center" prop="travelStatus" min-width="110">
        <template slot-scope="scope">
          <span v-if="scope.row.travelStatus == null">—</span>
          <el-dropdown
            v-else-if="nextTravelStatuses(scope.row.travelStatus).length"
            @command="status => handleTravelStatusChange(scope.row, status)"
          >
            <el-tag size="small" type="success">
              {{ travelStatusLabel(scope.row.travelStatus) }}
              <i class="el-icon-arrow-down el-icon--right"></i>
            </el-tag>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item
                v-for="item in nextTravelStatuses(scope.row.travelStatus)"
                :key="item.value"
                :command="item.value"
              >{{ item.label }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
          <el-tag v-else size="small" type="info">{{ travelStatusLabel(scope.row.travelStatus) }}</el-tag>
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
                <el-dropdown-item @click.native="handleUpdate(scope.row)"
                                  icon="el-icon-info"
                                  v-has-permi="['system:app_goods_order:edit']">修改</el-dropdown-item>
                <el-dropdown-item @click.native="handleDelete(scope.row)"
                                  icon="el-icon-edit"
                                  v-hasPermi="['system:app_goods_order:remove']">删除</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改商品订单对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="商品ID" prop="goodsId">
          <el-input v-model="form.goodsId" placeholder="请输入商品ID" />
        </el-form-item>
        <el-form-item label="所属分站" prop="deptId">
          <el-input v-model="form.deptId" placeholder="请输入所属分站" />
        </el-form-item>
        <el-form-item label="收货地址" prop="addressId">
          <el-input v-model="form.addressId" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="商品合计金额" prop="moneyTotal">
          <el-input v-model="form.moneyTotal" placeholder="请输入商品合计金额" />
        </el-form-item>
        <el-form-item label="折扣金额" prop="moneyDiscount">
          <el-input v-model="form.moneyDiscount" placeholder="请输入折扣金额" />
        </el-form-item>
        <el-form-item label="商品应付金额" prop="moneyPayable">
          <el-input v-model="form.moneyPayable" placeholder="请输入商品应付金额" />
        </el-form-item>
        <el-form-item label="快递费" prop="moneyExpress">
          <el-input v-model="form.moneyExpress" placeholder="请输入快递费" />
        </el-form-item>
        <el-form-item label="是否已支付" prop="payStatus">
          <el-radio-group v-model="form.payStatus">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付金额" prop="payMoney">
          <el-input v-model="form.payMoney" placeholder="请输入支付金额" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payType">
          <el-radio-group v-model="form.payType">
            <el-radio
              v-for="dict in dict.type.pay_type"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付时间" prop="payTime">
          <el-date-picker clearable
            v-model="form.payTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择支付时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="商品总数量" prop="goodsCount">
          <el-input v-model="form.goodsCount" placeholder="请输入商品总数量" />
        </el-form-item>
        <el-form-item label="使用优惠券的id集合，逗号分割" prop="couponGotIds">
          <el-input v-model="form.couponGotIds" placeholder="请输入使用优惠券的id集合，逗号分割" />
        </el-form-item>
        <el-form-item label="订单备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="发货时间" prop="sendTime">
          <el-date-picker clearable
            v-model="form.sendTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择发货时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="发货快递名称" prop="sendExpressName">
          <el-input v-model="form.sendExpressName" placeholder="请输入发货快递名称" />
        </el-form-item>
        <el-form-item label="发货快递标识" prop="sendExpressSimple">
          <el-input v-model="form.sendExpressSimple" placeholder="请输入发货快递标识" />
        </el-form-item>
        <el-form-item label="发货快递单号" prop="sendExpressNo">
          <el-input v-model="form.sendExpressNo" placeholder="请输入发货快递单号" />
        </el-form-item>
        <el-form-item label="核销码" prop="checkNum">
          <el-input v-model="form.checkNum" placeholder="请输入核销码" />
        </el-form-item>
        <el-form-item label="是否已核销" prop="isChecked">
          <el-radio-group v-model="form.isChecked">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="收货时间" prop="receiveTime">
          <el-date-picker clearable
            v-model="form.receiveTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择收货时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="是否已经发放奖励" prop="isAward">
          <el-radio-group v-model="form.isAward">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否已评论" prop="isComment">
          <el-radio-group v-model="form.isComment">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否申请取消" prop="isApplyCancel">
          <el-radio-group v-model="form.isApplyCancel">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="商户是否已结算" prop="isCash">
          <el-radio-group v-model="form.isCash">
            <el-radio
              v-for="dict in dict.type.common_is_not"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="订单状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.order_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApp_goods_order, getApp_goods_order, delApp_goods_order, addApp_goods_order, updateApp_goods_order, updateTravelStatus } from "@/api/system/app_goods_order";
export default {
  name: "App_goods_order",
  dicts: ['common_is_not', 'pay_type', 'order_status'],
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
      // 商品订单表格数据
      app_goods_orderList: [],
      travelStatusOptions: [
        { value: '0', label: '待确认' },
        { value: '1', label: '已确认' },
        { value: '2', label: '已取消' },
        { value: '3', label: '已入住' },
        { value: '4', label: '已离店' },
        { value: '5', label: '已结算' },
        { value: '6', label: '退款中' },
        { value: '7', label: '已退款' }
      ],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        goodsId: null,
        deptId: null,
        addressId: null,
        orderNo: null,
        moneyTotal: null,
        moneyDiscount: null,
        moneyPayable: null,
        moneyExpress: null,
        payStatus: null,
        payMoney: null,
        payType: null,
        payTime: null,
        goodsCount: null,
        couponGotIds: null,
        sendTime: null,
        sendExpressName: null,
        sendExpressSimple: null,
        sendExpressNo: null,
        checkNum: null,
        isChecked: null,
        receiveTime: null,
        isAward: null,
        isComment: null,
        isApplyCancel: null,
        isCash: null,
        status: null,
        travelStatus: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "用户ID不能为空", trigger: "blur" }
        ],
        goodsId: [
          { required: true, message: "商品ID不能为空", trigger: "blur" }
        ],
        deptId: [
          { required: true, message: "所属分站不能为空", trigger: "blur" }
        ],
        orderNo: [
          { required: true, message: "订单号不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "订单状态不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询商品订单列表 */
    getList() {
      this.loading = true;
      listApp_goods_order(this.queryParams).then(response => {
        this.app_goods_orderList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        orderId: null,
        userId: null,
        goodsId: null,
        deptId: null,
        addressId: null,
        orderNo: null,
        moneyTotal: null,
        moneyDiscount: null,
        moneyPayable: null,
        moneyExpress: null,
        payStatus: null,
        payMoney: null,
        payType: null,
        payTime: null,
        goodsCount: null,
        couponGotIds: null,
        remark: null,
        sendTime: null,
        sendExpressName: null,
        sendExpressSimple: null,
        sendExpressNo: null,
        checkNum: null,
        isChecked: null,
        receiveTime: null,
        isAward: null,
        isComment: null,
        isApplyCancel: null,
        isCash: null,
        createTime: null,
        updateTime: null,
        status: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    travelStatusLabel(status) {
      const option = this.travelStatusOptions.find(item => item.value === status);
      return option ? option.label : '未知状态';
    },
    nextTravelStatuses(status) {
      const transitions = {
        '1': ['3'],
        '3': ['4'],
        '4': ['5']
      };
      const targets = transitions[status] || [];
      return this.travelStatusOptions.filter(item => targets.includes(item.value));
    },
    handleTravelStatusChange(row, travelStatus) {
      updateTravelStatus(row.orderId, travelStatus).then(() => {
        this.$modal.msgSuccess('旅居状态已更新');
        this.getList();
      });
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.orderId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加商品订单";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const orderId = row.orderId || this.ids
      getApp_goods_order(orderId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改商品订单";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.orderId != null) {
            updateApp_goods_order(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addApp_goods_order(this.form).then(response => {
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
      const orderIds = row.orderId || this.ids;
      this.$modal.confirm('是否确认删除商品订单编号为"' + orderIds + '"的数据项？').then(function() {
        return delApp_goods_order(orderIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/app_goods_order/export', {
        ...this.queryParams
      }, `app_goods_order_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
