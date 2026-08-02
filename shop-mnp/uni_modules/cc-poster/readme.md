# cc-poster 注意： 1:长按保存图片不支持H5，小程序 App均支持  2:图片地址需要是小程序白名单设置的IP地址

##uniapp专属精品组件页面模板（由前端组件开发出品）精品组件页面模板

###●组件模板效果图：
可下载项目后预览，效果图见右侧图片；

###●组件模板费用：
学习：免费下载，进行学习，无费用；

使用/商用：本页面地址赞赏10元后，可终身商用；

###●组件模板使用版权/商用：
本组件模板免费下载可供学习，如需使用及商用，请在本组件页面模板进行赞赏10元

（仅需10元获取精品页面模板代码-物有所值，1个组件页面市场价100元 ）

赞赏10元后（当前项目产生赞赏订单可追溯）即可终身商用当前本地址下载的页面模版代码，不同下载地址需进行不同的赞赏。（不赞赏就进行商用使用者，面临侵权！保留追究知识产权法律责任！后果自负！）


### 我的技术公众号(私信可加前端技术交流群)

群内气氛颇为不错，应该可能算为数不多专门进行技术交流的前端群，同时也涵盖后端技术产品交流，偶尔成员们会聊天摸鱼。

![图片](https://i.postimg.cc/RZ0sjnYP/front-End-Component.jpg)



#### 使用方法 
```使用方法
<!-- 自定义生成海报组件  注意⚠️图片地址需要是小程序白名单设置的IP地址-->
<!-- @success：成功事件  imgSrc：图片地址 QrSrc：二维码图片地址  Title：标题 PriceTxt：价格 OriginalTxt：原始价格 LineType -->
<cc-poster @success="posterSuccess" :imgSrc="goods.itempic" :QrSrc="erweimapath" :Title="goods.itemtitle" :PriceTxt="goods.itemendprice" :OriginalTxt="goods.itemprice":LineType="false"></cc-poster>					
```

#### HTML代码实现部分
```html
<template>
	<view>
		<button style="margin: 66px 36px;" @click="showModal"> 生成海报 </button>

		<view class="cu-modal" :class="modalName == 'Image' ? 'show' : ''" @tap="hideModal">
			<view class="cu-dialog" v-if="goods && erweimapath && haibaoShow" @tap="hideModal">
				<view class="bg-img">

					<!-- 自定义生成海报组件 -->
					<!-- @success：成功事件  imgSrc：图片地址 QrSrc：二维码图片地址  Title：标题 PriceTxt：价格 OriginalTxt：原始价格 LineType -->
					<cc-poster @success="posterSuccess" :imgSrc="goods.itempic" :QrSrc="erweimapath"
						:Title="goods.itemtitle" :PriceTxt="goods.itemendprice" :OriginalTxt="goods.itemprice"
						:LineType="false"></cc-poster>
				</view>
			</view>
		</view>
	</view>
</template>
<script>
	//高德SDK
	export default {
		data() {
			return {
				haibaoImg: null,
				modalName: '',
				haibaoShow: false,
				//二维码图片地址
				erweimapath: "https://www.xinhuanet.com/politics/2016-07/19/5119875106653616178_11n.jpg",
				goods: {
					"itemid": "2nNozWXiotnRwxGUakBuXUD-0npyN5GSNmYDb0pgTxw",
					"itemtitle": "开丽产妇卫生巾产后专用刀纸月子安睡裤计量卫生巾孕妇用品安心裤",
					"itemshorttitle": "开丽产妇卫生巾月子安睡裤计量卫生",
					"itemdesc": "【开丽旗舰店】安心裤型亲肤面料，产后专用不勒刀口，免穿设计方便，超强吸收加长加大款，产妇的优质选择！【赠运费险】",
					"itemprice": "39.90",
					"todaysale": "3",
					"itempic": "https://img.alicdn.com/imgextra/i1/2742920221/O1CN01nFqqJ41DVGNfjYMmu_!!2742920221.jpg",
					"itemendprice": "29.91",

				}

			};
		},

		onLoad() {

		},
		methods: {
			posterSuccess(haibaoImg) {
				this.haibaoImg = haibaoImg;
				this.modalName = 'Image';
			},
			showModal() {
				if (!this.haibaoImg) {
					this.haibaoShow = true;

					uni.showLoading({
						title: '海报生成中...'
					});

				} else {
					this.modalName = 'Image';
				}
			},
			hideModal() {
				this.modalName = null;
			}

		}
	};
</script>
<style lang="scss">
	.cu-modal {
		position: fixed;
		top: 0;
		right: 0;
		bottom: 0;
		left: 0;
		z-index: 1110;
		opacity: 0;
		outline: 0;
		text-align: center;
		-ms-transform: scale(1.185);
		transform: scale(1.185);
		backface-visibility: hidden;
		perspective: 2000upx;
		background: rgba(0, 0, 0, 0.6);
		transition: all 0.3s ease-in-out 0s;
		pointer-events: none;
	}

	.cu-modal::before {
		content: "\200B";
		display: inline-block;
		height: 100%;
		vertical-align: middle;
	}

	.cu-modal.show {
		opacity: 1;
		transition-duration: 0.3s;
		-ms-transform: scale(1);
		transform: scale(1);
		overflow-x: hidden;
		overflow-y: auto;
		pointer-events: auto;
	}

	.cu-dialog {
		position: relative;
		display: inline-block;
		vertical-align: middle;
		margin-left: auto;
		margin-right: auto;
		width: 680upx;
		max-width: 100%;
		background-color: #f8f8f8;
		border-radius: 10upx;
		overflow: hidden;
	}
</style>


```

