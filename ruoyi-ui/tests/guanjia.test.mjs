import test from 'node:test';
import assert from 'node:assert/strict';
import {guanjiaUrl,exportQuery,rosterTarget} from '../src/utils/guanjia.mjs';

test('AI上下文仅携带任务与对象ID，同页导航保留现有登录，不传凭据或客户资料',()=>{
 const url=new URL(guanjiaUrl({task:'edit',type:'goods',id:311,from:'goods',token:'must-not-travel',name:'private-name'}));
 assert.equal(url.origin,'https://gatheringtech.com');assert.equal(url.search,'');
 assert.match(url.hash,/task=edit/);assert.match(url.hash,/id=311/);assert.doesNotMatch(url.href,/token|private-name/);
 for(const id of [0,-1,'abc',9007199254740992])assert.throws(()=>guanjiaUrl({task:'edit',type:'goods',id,from:'goods'}));
 assert.throws(()=>guanjiaUrl({task:'page',page:'settings',from:'goods'}));
});
test('导出沿用筛选而非当前页；商品、课程与活动名单精确过滤并要求原导出权限',()=>{
 assert.deepEqual(exportQuery({pageNum:2,pageSize:10,goodsType:'education',status:'1',params:{beginTime:'2026-10-16'}}),{goodsType:'education',status:'1',params:{beginTime:'2026-10-16'}});
 assert.deepEqual(rosterTarget({type:'goods',id:311}),{url:'system/app_goods_order/roster',query:{goodsId:311},permission:'system:app_goods_order:export'});
 assert.deepEqual(rosterTarget({type:'activity',id:7}).query,{activityId:7});assert.throws(()=>rosterTarget({type:'course',id:7}));
});
