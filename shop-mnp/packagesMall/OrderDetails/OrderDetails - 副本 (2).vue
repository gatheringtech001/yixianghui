<template>
  <view class="booking-page">
    <!-- 页面头部 -->
    <view class="header">
      <text class="title">预订信息</text>
      <navigator url="/pages/index/index" class="back-btn">
        <text class="iconfont icon-fanhui"></text>
      </navigator>
    </view>

    <!-- 房型信息 -->
    <view class="room-info">
      <image :src="roomImage" mode="aspectFill" class="room-img"></image>
      <view class="info-text">
        <text class="room-name">{{ roomName }}</text>
        <text class="room-desc">{{ roomDesc }}</text>
      </view>
    </view>

    <!-- 入住退房日期 -->
    <view class="date-picker">
      <view class="date-item">
        <text class="label">入住日期</text>
        <text class="value">{{ checkInDate }}</text>
      </view>
      <view class="duration">
        <text>{{ stayDays }}晚</text>
      </view>
      <view class="date-item">
        <text class="label">退房日期</text>
        <text class="value">{{ checkOutDate }}</text>
      </view>
    </view>

    <!-- 供餐套餐选择 -->
    <view class="meal-section">
      <view class="section-title">
        <text class="icon">●</text>
        <text>选择供餐套餐</text>
        <text class="price">￥{{ mealPrice }}/人/天</text>
      </view>
      <view class="meal-options">
        <view
            v-for="(option, index) in mealOptions"
            :key="index"
            class="meal-option"
            :class="{ active: selectedMeal === index }"
            @click="selectMeal(index)"
        >
          {{ option.name }}
          <text v-if="selectedMeal === index" class="check-icon">✓</text>
        </view>
      </view>
    </view>

    <!-- 餐厅地址 -->
    <view class="restaurant-info">
      <text class="label">供餐地址：</text>
      <text class="value">{{ restaurantAddress }}</text>
    </view>

    <!-- 预订信息 -->
    <view class="booking-info">
      <view class="section-title">
        <text class="icon">●</text>
        <text>预订信息</text>
      </view>

      <!-- 房间数量 -->
      <view class="input-group">
        <text class="label">定几间房</text>
        <view class="counter">
          <button class="minus-btn" @click="decreaseRooms">-</button>
          <input type="number" v-model="rooms" class="count-input" />
          <button class="plus-btn" @click="increaseRooms">+</button>
        </view>
        <text class="unit">间</text>
      </view>

      <!-- 用餐人数 -->
      <view class="input-group">
        <text class="label">几人用餐</text>
        <view class="counter">
          <button class="minus-btn" @click="decreasePeople">-</button>
          <input type="number" v-model="people" class="count-input" />
          <button class="plus-btn" @click="increasePeople">+</button>
        </view>
        <text class="unit">人</text>
      </view>

      <!-- 联系姓名 -->
      <view class="input-group">
        <text class="label">联系姓名</text>
        <input v-model="contactName" placeholder="请输入您的姓名" class="input-field" />
      </view>

      <!-- 联系电话 -->
      <view class="input-group">
        <text class="label">联系电话</text>
        <input v-model="contactPhone" placeholder="请输入您的手机号码" class="input-field" />
      </view>

      <!-- 提示信息 -->
      <view class="tips">
        <text class="tip-item">ℹ️ 为了三意旅居方便联系到您，请确保您输入的信息是正确的。</text>
        <text class="tip-item">ℹ️ 不占床位人员不记人数。1间房最多入住2人</text>
      </view>

      <!-- 预订须知 -->
      <view class="agreement">
        <checkbox v-model="agreeTerms" class="checkbox" />
        <text class="text">我已阅读 <text class="link">《预订及入住须知》</text></text>
      </view>
    </view>

    <!-- 底部支付栏 -->
    <view class="footer">
      <view class="amount">
        <text class="price">￥{{ totalAmount }}</text>
        <text class="label">实付金额</text>
      </view>
      <button class="pay-btn" @click="goToPay" :disabled="!agreeTerms || !contactName || !contactPhone">
        去付款
      </button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      roomImage: '/static/img/showver.jpg',
      roomName: '标准双人房/带飘窗【住满3晚送香港/澳门一日游一次，满6晚送香港/澳门各一次】',
      roomDesc: '深圳福田中心城区旅居基地（免费送香港/澳门一日游/近地铁口，近梅林水库、梅林公园）【年满60深圳公交地铁全免费】',
      checkInDate: '11月7日',
      checkOutDate: '11月10日',
      stayDays: '4天3晚',
      mealPrice: '0',
      mealOptions: [
        { name: '不含餐' },
        { name: '一早一晚（自助餐）' },
        { name: '一日三餐（自助餐）' },
        { name: '一日三餐（早餐自助+正餐套餐）' }
      ],
      selectedMeal: 0,
      rooms: 1,
      people: 2,
      contactName: '',
      contactPhone: '',
      agreeTerms: false,
      restaurantAddress: '基地餐厅'
    };
  },
  computed: {
    totalAmount() {
      // 示例计算逻辑：房间价格 * 天数 * 房间数 + 餐费
      const basePrice = 804; // 示例价格
      return basePrice;
    }
  },
  methods: {
    selectMeal(index) {
      this.selectedMeal = index;
    },
    increaseRooms() {
      this.rooms++;
    },
    decreaseRooms() {
      if (this.rooms > 1) this.rooms--;
    },
    increasePeople() {
      this.people++;
    },
    decreasePeople() {
      if (this.people > 1) this.people--;
    },
    goToPay() {
      if (!this.agreeTerms) {
        uni.showToast({
          title: '请先阅读并同意预订须知',
          icon: 'none'
        });
        return;
      }
      if (!this.contactName || !this.contactPhone) {
        uni.showToast({
          title: '请填写完整的联系信息',
          icon: 'none'
        });
        return;
      }
      uni.navigateTo({
        url: '/pages/CashierDesk/CashierDesk?amount=' + this.totalAmount
      });
    }
  }
};
</script>
<style scoped lang="scss">
@import "OrderDetails.scss";
</style>