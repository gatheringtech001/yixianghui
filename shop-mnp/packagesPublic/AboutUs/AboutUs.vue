<template>
	<view class="page">
		<scroll-view class="about-scroll" scroll-y :show-scrollbar="false">
			<view class="about-shell">
				<view class="hero-card">
					<text class="hero-brand">逸享荟</text>
					<text class="hero-title">让美好退休生活自然发生</text>
					<text class="hero-summary">陪您发现好去处、认识新朋友，安心享受每一段康养时光。</text>
				</view>

				<view class="intro-card">
					<text class="section-title">关于逸享荟</text>
					<text class="intro-text">逸享荟是上海智享居健康科技有限公司旗下康养品牌，专注为中老年朋友提供贴近生活、安心可靠的康养服务。</text>
				</view>

				<view class="service-section">
					<text class="section-title">我们为您提供</text>
					<view class="service-list">
						<view v-for="item in services" :key="item.title" class="service-card">
							<view class="service-badge">{{ item.badge }}</view>
							<view class="service-copy">
								<text class="service-title">{{ item.title }}</text>
								<text class="service-description">{{ item.description }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="promise-card">
					<text class="promise-label">我们的心愿</text>
					<text class="promise-title">让每一次出发更安心，让每一天生活更丰富。</text>
				</view>

				<view class="about-footer">
					<image class="footer-logo" src="/static/home-design/brand-logo-transparent.png" mode="aspectFit" />
					<text>上海智享居健康科技有限公司</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				services: [
					{ badge: '旅', title: '康养旅居', description: '精选适合长者的旅居目的地，住得舒心，玩得轻松。' },
					{ badge: '康', title: '社区康养', description: '把贴心服务带到家门口，让日常生活更便利。' },
					{ badge: '护', title: '专业照护', description: '以专业、细致的服务，守护长者和家人的安心。' }
				]
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'AboutUs.scss';
</style>
