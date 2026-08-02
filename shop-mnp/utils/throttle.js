function noMultipleClicks(fn, params, event) {
    let _this = this
    if (_this.onoff) {
        _this.onoff = false;
        if ((params && params!=='')||params==0) {
			if(event && event != undefined) fn(params, event)
			else fn(params)
        } else {
            fn()
        }
        setTimeout(()=>{
            _this.onoff = true;
        }, 1000)
    }
}
export default {
	noMultipleClicks
}