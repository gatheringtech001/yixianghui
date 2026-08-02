<template>
  <view class="calendar-container">
    <!-- Swiper 用于滚动切换月份 -->
    <swiper
        :indicator-dots="false"
        :autoplay="false"
        :interval="5000"
        :duration="500"
        :circular="true"
        vertical="true"
        @change="onSwiperChange"
		style="min-height: 850rpx;"
    >
      <swiper-item v-for="(month, index) in months" :key="index">
        <view class="month-header">
          {{ month.year }}年{{ month.month }}月
        </view>
        <view class="weekdays">
          <text>一</text>
          <text>二</text>
          <text>三</text>
          <text>四</text>
          <text>五</text>
          <text>六</text>
          <text>日</text>
        </view>
        <view class="days">
          <view
              v-for="(day, dayIndex) in month.days"
              :key="dayIndex"
              class="day-item"
              :class="{ 'today': day.isToday, 'selected': day.isSelected, 'in-range': isInRange(day.date) }"
              @click="selectDate(day)"
          >
            <text v-if="day.isToday" class="today-text">今天</text>
            <text class="day-number">{{ day.day }}</text>
            <text class="price-text" v-if="day.day">¥{{ pricePerPerson }}/人</text>
          </view>
        </view>
      </swiper-item>
    </swiper>

    <!-- 底部操作按钮 -->
    <view class="bottom-actions">
      <button class="clear-btn" @click="clearSelection()">清除</button>
      <button class="confirm-btn" @click="confirmSelection()">选择入住、离店日期</button>
    </view>
  </view>
</template>

<script>
export default {
  name: 'CustomCalendar',
  props: {
    minDate: { type: String, default: '' },
    maxDate: { type: String, default: '' },
    pricePerPerson: { type: Number, default: 134 },
    showToday: { type: Boolean, default: true }
  },
  data() {
    return {
      currentDate: new Date(),
      selectedStartDate: null,
      selectedEndDate: null,
      year: new Date().getFullYear(),
      month: new Date().getMonth(), // 0-11
      months: [],
      selectedDate: null
    };
  },
  mounted() {
    this.generateMonths();
  },
  methods: {
    // 判断当前日期是否在已选范围内
    isInRange(dateStr) {
      if (!this.selectedStartDate || !this.selectedEndDate) return false;
      const date = new Date(dateStr);
      const start = new Date(this.selectedStartDate);
      const end = new Date(this.selectedEndDate);
      return date >= start && date <= end;
    },
    // 选择日期，支持起止日期范围选择
    selectDate(day) {
      if (!day.date || !day.day) return;

      const selectedDate = new Date(day.date);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      if (selectedDate < today) return;

      if (!this.selectedStartDate) {
        this.selectedStartDate = day.date;
        this.selectedEndDate = null;
      } else if (!this.selectedEndDate) {
        this.selectedEndDate = day.date;
        this.$emit('date-range-selected', {
          start: this.selectedStartDate,
          end: this.selectedEndDate
        });
      }
    },

    // 生成初始月份数据
    generateMonths() {
      const current = new Date();
      const months = [];

      for (let i = 0; i < 3; i++) {
        const year = current.getFullYear();
        const month = current.getMonth() + i;
        const nextMonth = new Date(year, month + 1, 0);
        const daysInMonth = nextMonth.getDate();

        const days = [];
        const firstDay = new Date(year, month, 1);
        const startDay = firstDay.getDay(); // 0=周日

        // 填充前面空白格（上个月）
        for (let j = 0; j < startDay; j++) {
          days.push({ day: '', isToday: false, isSelected: false });
        }

        // 填充当前月日期
        for (let k = 1; k <= daysInMonth; k++) {
          const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(k).padStart(2, '0')}`;
          const isToday = this.isSameDay(dateStr, new Date());
          const isSelected = this.selectedDate && this.selectedDate === dateStr;

          days.push({
            day: k,
            isToday,
            isSelected,
            date: dateStr
          });
        }

        // 添加下个月的日期（填充剩余位置）
        const totalCells = 42;
        const remaining = totalCells - days.length;
        for (let i = 1; i <= remaining; i++) {
          days.push({ day: i, isToday: false, isSelected: false });
        }

        months.push({
          year,
          month: month + 1,
          days
        });
      }

      this.months = months;
    },

    // 判断两个日期是否是同一天（忽略时间部分）
    isSameDay(date1, date2) {
      const d1 = new Date(date1);
      const d2 = new Date(date2);

      if (isNaN(d1.getTime()) || isNaN(d2.getTime())) {
        return false;
      }

      return d1.toISOString().split('T')[0] === d2.toISOString().split('T')[0];
    },

    // Swiper 切换时触发，动态加载下一个月
    onSwiperChange(e) {
      const currentIndex = e.detail.current;
      if (currentIndex >= this.months.length - 1) {
        this.generateNextMonth();
      }
    },

    // 动态生成下一个月
    generateNextMonth() {
      const lastMonth = this.months[this.months.length - 1];
      const nextMonth = new Date(lastMonth.year, lastMonth.month, 1);
      nextMonth.setMonth(nextMonth.getMonth() + 1);

      const year = nextMonth.getFullYear();
      const month = nextMonth.getMonth() + 1;
      const nextMonthEnd = new Date(year, month, 0);
      const daysInMonth = nextMonthEnd.getDate();

      const days = [];
      const firstDay = new Date(year, month - 1, 1);
      const startDay = firstDay.getDay();

      for (let j = 0; j < startDay; j++) {
        days.push({ day: '', isToday: false, isSelected: false });
      }

      for (let k = 1; k <= daysInMonth; k++) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(k).padStart(2, '0')}`;
        const isToday = this.isSameDay(dateStr, new Date());
        const isSelected = this.selectedDate && this.selectedDate === dateStr;

        days.push({
          day: k,
          isToday,
          isSelected,
          date: dateStr
        });
      }

      this.months.push({
        year,
        month,
        days
      });
    },

    // 清除选择
    clearSelection() {
      this.selectedStartDate = null;
      this.selectedEndDate = null;
      this.$emit('clear-selection');
    },

    // 确认选择
    confirmSelection() {
      if (this.selectedStartDate && this.selectedEndDate) {
        this.$emit('confirm-selection', {
          start: this.selectedStartDate,
          end: this.selectedEndDate
        });
      }
    }
  }
};
</script>

<style scoped lang="scss">
@import './CustomCalendar.scss';
</style>
