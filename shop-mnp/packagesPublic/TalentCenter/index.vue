<template>
	<web-view :src="talentCenterUrl" />
</template>

<script>
	const TALENT_CENTER_URL = 'https://gatheringtech.com/talent/'

	export default {
		data() {
			return {
				talentCenterUrl: TALENT_CENTER_URL
			}
		}
	}
</script>
