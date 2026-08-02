import Vue from 'vue';
import App from './App';
import BaseUrl from './api/baseUrl.js';
import DialogBox from './components/DialogBox/DialogBox';
import uView from "uview-ui";
import MescrollBody from "@/components/mescroll-uni/mescroll-body.vue"
import MescrollUni from "@/components/mescroll-uni/mescroll-uni.vue"

Vue.use(uView);

Vue.config.productionTip = false
Vue.prototype.$host = BaseUrl.publicUrl.replace(/\/$/, '')

Vue.component('DialogBox', DialogBox);
Vue.component('mescroll-body', MescrollBody)
Vue.component('mescroll-uni', MescrollUni)

App.mpType = 'app'

const app = new Vue({
    ...App
})
app.$mount()
