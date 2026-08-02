
<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>import * as echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'

export default {
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '250px'
    },
    // 新增：接收图表数据（使用 kebab-case 命名）
    chartData: {
      type: Object,
      default: () => []
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    // 监听数据变化，自动更新图表
    chartData: {
      handler() {
        this.updateChart();
      },
      deep: true
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
    })
  },
  beforeDestroy() {
    if (!this.chart) {
      return
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')
      this.updateChart()
    },
    updateChart() {
      // 使用传递的数据或默认数据
      const data = this.chartData && this.chartData.length > 0
        ? this.chartData
        : [
          { value: 274, name: '男', itemStyle: { color: '#ec8e04' } },
          { value: 322, name: '女', itemStyle: { color: '#d58004' } },
          { value: 141, name: '空值', itemStyle: { color: '#E6A23C' } }
        ]

      const option = {
        title: {
          text: '男女比例',
          left: 'left',
          textStyle: {
            color: '#0e0e0e'
          }
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c}人 ({d}%)'
        },
        legend: {
          left: 'left',
          top: '35',
          data: data.map(item => item.name),
          icon: 'circle'
        },
        series: [
          {
            name: '性别构成',
            type: 'pie',
            radius: [0, 55],
            top: '70',
            center: ['50%', '38%'],
            data: data,
            label: {
              show: true,
              formatter: '{b}:{c}({d}%)',
              position: 'outside'
            },
            labelLine: {
              show: false
            },
            animationEasing: 'cubicInOut',
            animationDuration: 2600
          }
        ]
      }

      this.chart.setOption(option, true)
    }
  }
}
</script>
