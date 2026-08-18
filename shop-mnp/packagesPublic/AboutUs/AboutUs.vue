<template>
	<view class="page">
		<scroll-view class="about-scroll" scroll-y :show-scrollbar="false">
			<view class="about-shell">
				<view class="hero-card">
					<image class="hero-mark" src="/static/home-design/brand-logo-transparent.png" mode="aspectFit" />
					<text class="hero-company">上海智享居健康科技有限公司</text>
					<text class="hero-title">您身边的康养管家</text>
					<text class="hero-summary">以家庭为核心、以社区为依托，为长者提供家门口的一站式康养管家服务。</text>
					<view class="hero-meta">
						<text>逸享荟康养品牌</text>
						<text>资料更新 2025.10</text>
					</view>
				</view>

				<view class="stats-grid">
					<view v-for="item in stats" :key="item.label" class="stat-card">
						<text class="stat-value">{{ item.value }}</text>
						<text class="stat-label">{{ item.label }}</text>
					</view>
				</view>

				<view class="content-section">
					<text class="section-kicker">ABOUT US</text>
					<text class="section-title">让专业服务更靠近长者</text>
					<text class="section-body">上海智享居健康科技有限公司成立于 2022 年，面向广大中老年人开展社区居家养老服务。2025 年，公司成立“逸享荟”康养品牌，持续连接居家、社区、机构与旅居场景。</text>
					<view class="mission-card">
						<text class="mission-label">我们的定位</text>
						<text class="mission-text">依托社区居家养老服务站，打造小平台、多功能的金牌康养管家模式，围绕长者需求提供多样化服务。</text>
					</view>
				</view>

				<view class="content-section">
					<text class="section-kicker">OUR SERVICES</text>
					<text class="section-title">五大业务板块</text>
					<view class="service-list">
						<view v-for="(item, index) in services" :key="item.title" class="service-card">
							<text class="service-index">{{ index + 1 < 10 ? '0' + (index + 1) : index + 1 }}</text>
							<view class="service-copy">
								<text class="service-title">{{ item.title }}</text>
								<text class="service-description">{{ item.description }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="care-card">
					<text class="care-kicker">双翼服务模式</text>
					<text class="care-title">金牌康养管家 × 金牌康复护理</text>
					<view class="care-divider"></view>
					<text class="care-description">一端贴近日常生活与客户需求，一端提供专业、细致的康复护理，全方位回应长者需要。</text>
				</view>

				<view class="content-section">
					<text class="section-kicker">OUR JOURNEY</text>
					<text class="section-title">一路扎根社区，持续拓展服务</text>
					<view class="timeline">
						<view v-for="item in timeline" :key="item.year" class="timeline-item">
							<view class="timeline-axis">
								<view class="timeline-dot"></view>
								<view class="timeline-line"></view>
							</view>
							<view class="timeline-copy">
								<text class="timeline-year">{{ item.year }}</text>
								<text class="timeline-text">{{ item.text }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="value-card">
					<text class="value-label">逸享荟产品价值观</text>
					<text class="value-title">打造退休者旅居、文化、社交第一平台</text>
					<text class="value-description">倡导退休者拥抱变化，开启幸福退休生活。</text>
				</view>

				<view class="about-footer">
					<image src="/static/home-design/brand-logo-transparent.png" mode="aspectFit" />
					<text>上海智享居健康科技有限公司</text>
					<text class="footer-date">资料更新于 2025 年 10 月</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				stats: [
					{ value: '2022', label: '始于 2022' },
					{ value: '50+', label: '全国 50+ 旅居基地' },
					{ value: '5', label: '五大业务板块' }
				],
				services: [
					{ title: '社区居家养老', description: '以社区服务站为平台，连接助餐、陪诊、探访、康复护理与兴趣课堂等日常服务。' },
					{ title: '康养旅居', description: '面向退休人群提供旅居产品、体验服务与反向定制，持续建设全国服务网络。' },
					{ title: '社区嵌入式医养机构', description: '打造小规模、多功能、社区嵌入式、医养结合型的连锁康养机构。' },
					{ title: '智慧享老', description: '以智能设备和健康数据分析支持安全预警、健康监测与居家照护。' },
					{ title: '适老化设计改造', description: '围绕卫生间、淋浴间、卧室和厨房等生活空间，提升居住安全与便利。' }
				],
				timeline: [
					{ year: '2022', text: '上海智享居健康科技有限公司成立，开启社区居家养老服务探索。' },
					{ year: '2023', text: '昆明佳园、席子营社区居家养老服务站相继营业。' },
					{ year: '2024', text: '昆明金色社区服务站营业，并参与运营曲靖寥廓街道综合养老服务中心。' },
					{ year: '2025', text: '成立“逸享荟”康养品牌，上海张庙、昆明和平路社区服务站营业，并拓展国际居家护理合作。' }
				]
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'AboutUs.scss';
</style>
