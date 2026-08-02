<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>
import * as echarts from 'echarts'
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
    // 新增：接收图表数据
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
    // 监听数据变化
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
      const data = this.chartData.ydata && this.chartData.ydata.length > 0
        ? this.chartData.ydata
        : [93, 532, 98, 2, 4, 8]

      const xAxisLabels = this.chartData.xdata && this.chartData.xdata.length > 0
        ? this.chartData.xdata
        : ['佳园社区站点', '席子营社区站点', '金色社区站点', '外部', '总部', '空值']

      const option = {
        title: {
          text: '客户数量',
          left: 'left',
          textStyle: {
            color: '#0e0e0e'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: xAxisLabels,
          axisTick: {
            alignWithLabel: true
          },
          axisLabel: {
            rotate: 45,
            interval: 0,
            margin: 20,
            color: '#4a4949'
          },
          axisLine: {
            lineStyle: {
              color: '#4a4949',
              width: 1
            }
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            color: '#4a4949'
          }
        },
        legend: {
          data: ['客户数量'],
          left: 'left',
          top: '28',
          icon: 'circle',
          formatter: function(name) {
            return '计数';
          }
        },
        series: [{
          name: '客户数量',
          type: 'bar',
          barWidth: '30',
          data: data,
          itemStyle: {
            color: '#f6af46'
          }
        }]
      }

      this.chart.setOption(option, true)
    }
  }
}
</script>
