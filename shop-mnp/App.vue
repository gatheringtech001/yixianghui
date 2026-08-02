<script>
import { parseLaunchInviteOptions } from '@/utils/invite'

export default {
	onLaunch: function(options) {
		parseLaunchInviteOptions(options)
	},
	onShow: function(options) {
		parseLaunchInviteOptions(options)
	},
	onHide: function() {
		console.log('App Hide');
	}
};
</script>

<style>
/*每个页面公共css */
@import 'colorui/main.css';
@import 'colorui/icon.css';
@import 'style/FontStyle.css';

body{
	-webkit-text-size-adjust:none;
}
/* 超出一行省略号 */
.one-omit {
	white-space: nowrap; /*规定段落中的文本不进行换行*/
	overflow: hidden; /*内容会被修剪，并且其余内容是不可见的。*/
	text-overflow: ellipsis; /*显示省略号来代表被修剪的文本*/
}

.two-omit {
	display: -webkit-box;
	-webkit-line-clamp: 2;
	overflow: hidden;
	text-overflow: ellipsis;
	-webkit-box-orient: vertical;
}
img{
	width: 100%;
}
</style>
<style lang="scss">
	/* 注意要写在第一行，同时给style标签加入lang="scss"属性 */
	@import "uview-ui/index.scss";
	.weapp-nav-box{
		.u-slot-content {
			width: 100%;
			min-width: 0;
		}

		.slot-wrap{
			display: flex;
			align-items: center;
			width: 100%;
			box-sizing: border-box;
			image{
				width: auto;
				height: 88rpx;
				margin: 0 20rpx;
			}
		}

		.app-topbar {
			display: grid;
			grid-template-columns: 88rpx 1fr 184rpx;
			align-items: center;
			width: 100%;
			min-height: 88rpx;
			padding: 0 28rpx;
			box-sizing: border-box;

			&.compact {
				min-height: 88rpx;
			}

			.app-topbar-side {
				grid-column: 1;
				width: 88rpx;
			}

			.app-topbar-title {
				grid-column: 2;
				text-align: center;
				font-size: 40rpx;
				font-weight: 800;
				color: #111111;
				line-height: 1.25;
				white-space: nowrap;
				overflow: hidden;
				text-overflow: ellipsis;
			}

			.app-topbar-action {
				grid-column: 3;
				justify-self: end;
				display: inline-flex;
				align-items: center;
				justify-content: center;
				gap: 12rpx;
				min-height: 56rpx;
				padding: 0 22rpx;
				border: 1rpx solid #701018;
				border-radius: 999rpx;
				background: rgba(255, 255, 255, 0.92);
				color: #701018;
				font-size: 22rpx;
				font-weight: 750;
				white-space: nowrap;
				box-sizing: border-box;

				text {
					white-space: nowrap;
					line-height: 1;
				}
			}
		}

		&.home-nav .slot-wrap,
		&.service-nav .slot-wrap {
			gap: 18rpx;
			min-height: 84rpx;
			padding: 0 28rpx;

			.brand-logo {
				width: 152rpx !important;
				height: 80rpx !important;
				margin: 0 !important;
				flex-shrink: 0;
				border-radius: 0;
			}

			.brand-title {
				font-size: 40rpx;
				font-weight: 800;
				color: #111111;
				line-height: 1.25;
				white-space: nowrap;
			}
		}

		&.coins-nav,
		&.support-nav,
		&.detail-nav {
			::v-deep .u-title {
				font-weight: 800 !important;
				text-align: center;
			}
		}

		&.detail-nav {
			::v-deep .u-border-bottom:after {
				border-bottom-color: rgba(232, 225, 216, 0.7);
			}
		}
	}
</style>
