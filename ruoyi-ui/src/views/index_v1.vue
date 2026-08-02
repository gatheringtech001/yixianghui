<template>
  <div class="dashboard-editor-container">

  <panel-group/>

<!--    <el-row style="background:#fff;padding:16px 16px 0;margin-bottom:32px;">
      <line-chart :chart-data="lineChartData" />
    </el-row>-->
    <el-row :gutter="32">
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <div class="card-panel-value large-text">总数</div>
          <div class="card-panel-value large-number">{{ totalCustomerNum }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <long-insure-pie :chart-data="longInsureData"/>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <customer-num-statistic-pie :chart-data="customerNumStatisticData"/>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="32">
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <customer-gender-chart :chart-data="customerGenderData"/>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <residential-chart :chart-data="residentialData"/>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <customer-goods-pie :chart-data="customerGoodsData"/>
        </div>
      </el-col>
    </el-row>


  </div>
</template>

<script>
import PanelGroup from './dashboard/PanelGroup'
import LineChart from './dashboard/LineChart'
import {getCharData} from '@/api/index_v1'
import CustomerGenderChart from './dashboard/CustomerGenderPie.vue'
import ResidentialChart from './dashboard/ResidentialColumnShape.vue'
import CustomerGoodsPie from "@/views/dashboard/CustomerGoodsPie.vue"
import LongInsurePie from "@/views/dashboard/LongInsurePie.vue"
import CustomerNumStatisticPie from "@/views/dashboard/CustomerNumStatisticPie.vue"

const lineChartData = {
  newVisitis: {
    expectedData: [100, 120, 161, 134, 105, 160, 165],
    actualData: [120, 82, 91, 154, 162, 140, 145]
  },
  messages: {
    expectedData: [200, 192, 120, 144, 160, 130, 140],
    actualData: [180, 160, 151, 106, 145, 150, 130]
  },
  purchases: {
    expectedData: [80, 100, 121, 104, 105, 90, 100],
    actualData: [120, 90, 100, 138, 142, 130, 130]
  },
  shoppings: {
    expectedData: [130, 140, 141, 142, 145, 150, 160],
    actualData: [120, 82, 91, 154, 162, 140, 130]
  }
}

export default {
  name: 'Index',
  components: {
    CustomerGoodsPie,
    PanelGroup,
    LineChart,
    CustomerGenderChart,
    ResidentialChart,
    LongInsurePie,
    CustomerNumStatisticPie
  },
  // 在适当时候获取数据
  async mounted() {
    await this.loadChartData();
  },
  data() {
    return {
      lineChartData: lineChartData.newVisitis,
      longInsureData: {},
      residentialData: {},
      customerNumStatisticData: {},
      customerGenderData: {},
      customerGoodsData: {},
      totalCustomerNum: 0
    }
  },
  methods: {
    handleSetLineChartData(type) {
      this.lineChartData = lineChartData[type]
    },
    async loadChartData() {
      getCharData().then(res => {
        this.totalCustomerNum = res.data.totalcount
        this.customerGoodsData = res.data.goodsStatic
        this.residentialData = res.data.residentialStatic
        this.longInsureData = res.datainsureEvaStatic
        this.customerNumStatisticData = res.data.numStatic
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-editor-container {
  padding: 2px;
  background-color: rgb(240, 242, 245);
  position: relative;

  .chart-wrapper {
    background: #fff;
    padding: 16px 16px 0;
    margin-bottom: 5px;
  }
}

@media (max-width:1024px) {
  .chart-wrapper {
    padding: 8px;
  }
}

.large-text {
  font-size: 54px;
  text-align: left;
  color: #da922a;
}

.large-number {
  font-size: 148px;
  font-weight: bold;
  text-align: left;
  color: #f6af46;
  margin-top: 10px;
}
</style>
