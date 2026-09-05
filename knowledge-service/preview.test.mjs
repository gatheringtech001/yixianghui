import assert from "node:assert/strict";
import { test } from "node:test";
import { mediaDescriptor, openFeishuMedia } from "./preview.mjs";

const token = "QFHyb2GxUo0OHrxnA4AcREWVnyc";
test("only indexed Feishu file and document media can become previews", () => {
  assert.deepEqual(mediaDescriptor({sourceUrl:`https://vcnnjnb870d6.feishu.cn/file/${token}`,media:{kind:"video",fileToken:token}}),{kind:"video",token,type:"files"});
  assert.deepEqual(mediaDescriptor({url:"https://vcnnjnb870d6.feishu.cn/docx/document",media:{kind:"image",fileToken:token}}),{kind:"image",token,type:"medias"});
  for(const source of [
    {url:"http://127.0.0.1/private",media:{kind:"image",fileToken:token}},
    {url:"https://evil.example/file/abc",media:{kind:"video",fileToken:token}},
    {url:"https://vcnnjnb870d6.feishu.cn/file/different",media:{kind:"video",fileToken:token}},
    {url:"https://vcnnjnb870d6.feishu.cn/docx/doc",media:{kind:"image",fileToken:"../etc/passwd"}},
  ]) assert.equal(mediaDescriptor(source),null);
});
test("preview refreshes expired credentials and forwards byte ranges without redirects", async () => {
  const source={accessToken:"expired",tokenExpiresAt:0,authenticate:async()=>{source.accessToken="private-current-token";source.tokenExpiresAt=Date.now()+60000;}};
  let call;
  const response = await openFeishuMedia(source,{kind:"video",token,type:"files"},{range:"bytes=0-1023",fetcher:async(url,options)=>{
    call={url,options};return new Response(new Uint8Array(1024),{status:206,headers:{"content-type":"video/mp4","content-range":"bytes 0-1023/4096","content-length":"1024"}});
  }});
  assert.equal(response.status,206);
  assert.equal(call.options.headers.Range,"bytes=0-1023");
  assert.equal(call.options.headers.Authorization,"Bearer private-current-token");
  assert.equal(call.options.redirect,"error");
  assert.equal(call.url,`https://open.feishu.cn/open-apis/drive/v1/files/${token}/download`);
});
test("invalid ranges fail before contacting Feishu",async()=>{
  let calls=0;
  for(const range of ["bytes=0-1,3-4","bytes=9-1","garbage"]){
    await assert.rejects(openFeishuMedia({}, {kind:"image",token,type:"medias"},{range,fetcher:async()=>{calls++;}}),{status:416});
  }
  assert.equal(calls,0);
});
test("JSON errors and active image formats cannot masquerade as playable media",async()=>{
  const source={accessToken:"token",tokenExpiresAt:Date.now()+60000};
  for(const type of ["application/json","text/html","image/svg+xml"]){
    await assert.rejects(openFeishuMedia(source,{kind:"image",token,type:"medias"},{fetcher:async()=>new Response("bad",{headers:{"content-type":type}})}),{status:502});
  }
});
