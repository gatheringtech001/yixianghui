<template>
  <view class="page">
    <!-- 订单头部 -->
    <view class="order-header">
      <text class="order-no">订单号：58322</text>
      <navigator url="/pages/index/index" class="back-btn">
        <text class="iconfont icon-fanhui"></text>
      </navigator>
    </view>

    <!-- 房源信息 -->
    <view class="property-info">
      <view class="info-title">
        <text class="title-icon">|</text>
        <text class="title-text">房源信息</text>
      </view>
      <view class="property-content">
        <image :src="propertyImage" mode="aspectFill" class="property-img"></image>
        <view class="property-detail">
          <text class="property-name">{{ propertyName }}</text>
          <text class="property-desc">{{ propertyDesc }}</text>
          <text class="room-type">{{ roomType }}</text>
        </view>
      </view>
    </view>

    <!-- 预订信息 -->
    <view class="booking-info">
      <view class="info-title">
        <text class="title-icon">|</text>
        <text class="title-text">预订信息</text>
      </view>
      <view class="booking-content">
        <view class="date-info">
          <text class="date-label">入住：</text>
          <text class="date-value">{{ checkInDate }}</text>
          <text class="duration">{{ stayDays }}</text>
          <text class="date-label">退房：</text>
          <text class="date-value">{{ checkOutDate }}</text>
        </view>
        <view class="contact-info">
          <text class="contact-name">{{ contactName }}</text>
          <text class="contact-phone">{{ contactPhone }}</text>
        </view>
        <view class="booking-details">
          <text>{{ bookingDetails }}</text>
        </view>
        <view class="booking-time">
          <text>{{ bookingTime }}</text>
        </view>
      </view>
    </view>

    <!-- 金额信息 -->
    <view class="amount-info">
      <view class="info-title">
        <text class="title-icon">|</text>
        <text class="title-text">金额信息</text>
      </view>
      <view class="amount-content">
        <view class="amount-item">
          <text class="amount-label">总金额：</text>
          <text class="amount-value">￥{{ totalAmount }}</text>
        </view>
        <view class="amount-item">
          <text class="amount-label">全款：</text>
          <text class="amount-value">￥{{ totalAmount }}</text>
        </view>
        <view class="amount-item">
          <text class="amount-label">已付总额：</text>
          <text class="amount-value">￥0</text>
        </view>
        <view class="amount-item">
          <text class="amount-label">未付金额：</text>
          <text class="amount-value" style="color: #FF3B30;">￥{{ totalAmount }}</text>
        </view>
      </view>
    </view>

    <!-- 支付按钮 -->
    <view class="payment-footer">
      <view class="count-down">
        <text>订单保留</text>
        <text class="time">{{ countdown }}</text>
      </view>
      <button class="pay-btn" @click="goToPay">
        订单付款
      </button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      propertyImage: '/static/img/showver.jpg',
      propertyName: '深圳福田中心城区旅居基地（免费送香港/澳门一日游/近地铁口，近梅林水库、梅林公园）',
      propertyDesc: '标准双人房/带飘窗【住满3晚送香港/澳门一日游一次，满6晚送香港/澳门各一次】',
      roomType: '标准双人房/带飘窗',
      checkInDate: '2025年11月08日',
      checkOutDate: '2025年11月11日',
      stayDays: '4天3晚',
      contactName: 'test',
      contactPhone: '13769161332',
      bookingDetails: '已订1间；2人用餐，不含餐；',
      bookingTime: '2025年11月06日 预订',
      totalAmount: '804.00',
      countdown: '24:27'
    };
  },
  methods: {
    goToPay() {
      // 跳转到支付页面
      uni.navigateTo({
        url: '/packagesMall/CashierDesk/CashierDesk?orderAmount=' + this.totalAmount + '&orderId=58322'
      });
    }
  }
};
</script>

<style scoped lang="scss">
@import 'CashierDesk.scss';
</style>
