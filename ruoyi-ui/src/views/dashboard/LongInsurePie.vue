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
    // 新增：接收图表数据（支持多种格式）
    chartData: {
      type: Object,
      default: () => ({})
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
      const defaultData = {
        '佳园社区站点': [2, 4, 2, 2, 6, 10, 73],
        '席子营社区站点': [10, 245, 1, 1, 0, 206, 63],
        '金色社区站点': [0, 1, 1, 0, 0, 1, 96],
        '外部': [1, 0, 1, 1, 1, 0, 0]
      }

      const seriesData = this.chartData.insureEvaStaticData
      && this.chartData.insureEvaStaticData.length > 0
        ? this.chartData.insureEvaStaticData
        : defaultData

      const xAxisLabels = this.chartData.insureEvaStaticXKeys && this.chartData.insureEvaStaticXKeys.length > 0
        ? this.chartData.insureEvaStaticXKeys
        : ['社保不满足 身体满足', '社保满足 身体不满足', '已交资料', '已评定', '已服务', '其他', '空值']

      const legendNames = this.chartData.insureEvaStaticLegend && this.chartData.insureEvaStaticLegend.length > 0
        ? this.chartData.insureEvaStaticLegend
        : ['佳园社区站点', '席子营社区站点', '金色社区站点', '外部']

      // 定义颜色映射
      const colorMap = {
        '佳园社区站点': '#044a9a',
        '席子营社区站点': '#c57704',
        '金色社区站点': '#04dbe1',
        '外部': '#5bdd05'
      }

      const series = legendNames.map((name, index) => ({
        name: name,
        type: 'bar',
        barWidth: '3',
        data: seriesData[name] || [],
        itemStyle: {
          color: colorMap[name] || '#f6af46'
        }
      }))

      const option = {
        title: {
          text: '长护险状态',
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
            margin: 10,
            color: '#4a4949',
            height: 5,
            overflow: 'truncate',
            formatter: function(value) {
              // 截取前8个字符，超出部分用...表示
              return value.length > 8 ? value.substring(0, 8) + '...' : value;
            }
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
          },
          splitLine: {
            lineStyle: {
              type: 'dashed'
            }
          }
        },
        legend: {
          data: legendNames,
          left: 'left',
          top: '28',
          icon: 'circle',
          itemWidth: 12,
          itemHeight: 12,
          itemGap: 5
        },
        series: series
      }

      this.chart.setOption(option, true)
    }
  }
}
</script>
