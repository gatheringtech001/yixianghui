<template>
	<view class="page">
		<view class="logo">
			<image src="../../static/img/logo.jpg" mode=""></image>
		</view>
		<!-- 填写区 -->
		<!-- <view class="input-info">
      <view class="info">
        <input type="tel" maxlength="11" v-model="form.phone" placeholder="账号/手机号">
        <view class="more"></view>
      </view>
      <view class="info" :style="isLoginWay?'':'display: none'">
        <input type="tel" v-model="form.code" maxlength="6" placeholder="请输入验证码">
        <view class="more">
          <text class="mo">获取验证码</text>
          <text class="mo" style="display: none">59秒后重试</text>
        </view>
      </view>
      <view class="info" :style="isLoginWay?'display: none':''">
        <input :password='!isPassword' v-model="form.password" maxlength="26" placeholder="请输入密码">
        <view class="more">
          <text class="iconfont" :class="isPassword?'icon-eye-on':'icon-eye-off'" @click="isPassword = !isPassword"></text>
          <text class="mo">忘记密码</text>
        </view>
      </view>
    </view> -->
		<!-- 按钮 -->
		<!-- <view class="btn-info">
      <view class="btn" :style="isLogin?'opacity:1':'opacity:0.4'" @click="isLogin?onLogin():''">
        <text>登录</text>
      </view>
    </view> -->
		<!-- 操作 -->
		<!-- <view class="operation">
      <text @click="onLoginCut">{{isLoginWay?'手机号密码登录':'短信验证码登录'}}</text>
      <text @click="onRegister">新用户注册</text>
    </view> -->
		<!-- 其他方式登录 -->
		<!-- <view class="other-ways">
      <text>其他登录方式</text>
    </view> -->
		<view class="other-ways">
			<text>微信授权登录</text>
		</view>
		<!-- 登录方式 -->
		<view class="login-way">
			<view class="way" @click="$u.throttle(wxLogin, 500)">
				<image src="/static/wx_ico.png" mode=""></image>
				<text>微信登录</text>
			</view>
		</view>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import { ensureLogin, bindPageAuthPopup, syncConsultantStorage } from '@/utils/login'
	export default {
		components: {
			AuthProfilePopup
		},
		data() {
			return {
				isLogin: false,
				isLoginWay: false,
				isPassword: false,
				// 表单
				form: {
					phone: '',
					code: '',
					password: '',
				},
			};
		},
		onReady() {
			bindPageAuthPopup(this)
		},
		methods: {
			onRegister() {
				uni.navigateTo({
					url: '/packagesPublic/register/register'
				})
			},
			/**
			 * 登录切换
			 */
			onLoginCut() {
				this.isLoginWay = !this.isLoginWay;
				// 验证码
				if (this.isLoginWay) {
					this.isLogin = this.form.code && this.form.phone ? true : false;
				}
				// 账号密码
				if (!this.isLoginWay) {
					this.isLogin = this.form.password && this.form.phone ? true : false;
				}
			},
			/**
			 * 登录点击
			 */
			onLogin() {
				this.$u.throttle(() => {
					let params = {
						username: this.form.phone,
						password: this.form.password
					}
					login(params).then(res => {
						saveToken(res.token)
						this.getUserInfo()
					})
				}, 500)
			},
			getUserInfo() {
				getInfo().then(res => {
					syncConsultantStorage(res.consultant)
					uni.setStorageSync('userCard', res.userCard)
					uni.setStorageSync('userInfo', res.data)
					uni.setStorageSync('userData', res.userInfo)
					if (!res.data.mobile) {
						uni.showModal({
							title: '温馨提示',
							content: '是否去完善个人信息？',
							showCancel: true,
							cancelText: '暂不',
							cancelColor: '#000000',
							confirmText: '立即完善',
							confirmColor: '#FE6F1B',
							success: res => {
								if (res.confirm) {
									uni.redirectTo({
										url: '/packagesPublic/Information/Information?type=login'
									})
								} else if (res.cancel) {
									uni.navigateBack()
								}
							},
						})
					} else {
						uni.navigateBack()
					}
				})
			},
			wxLogin() {
				ensureLogin(this).then((ok) => {
					if (ok) {
						uni.navigateBack()
					}
				})
			},
		},
		watch: {
			form: {
				handler(newValue, oldValue) {
					// 验证码
					if (this.isLoginWay) {
						this.isLogin = newValue.code && newValue.phone ? true : false;
					}
					// 账号密码
					if (!this.isLoginWay) {
						this.isLogin = newValue.password && newValue.phone ? true : false;
					}
				},
				deep: true
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'login.scss';
</style>