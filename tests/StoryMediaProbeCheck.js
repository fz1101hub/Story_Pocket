/* Offline regression: the exact Story ID controls embedded video selection. */
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const source=fs.readFileSync(path.join(__dirname,'../app/src/main/java/com/storypocket/mobile/MainActivity.java'),'utf8');
const begin=source.indexOf('String script =');
const end=source.indexOf(';\n        script = script.replace',begin);
assert.ok(begin>=0 && end>begin);
const strings=source.slice(begin,end).match(/"(?:\\.|[^"\\])*"/g);
const script=strings.map(s=>JSON.parse(s)).join('');
new vm.Script(script);
const image='https://a.cdninstagram.com/cover.jpg',video='https://a.cdninstagram.com/story_high.mp4';
const movieId='123456789012345678';
function elem(kind,url){return {currentSrc:url,src:url,style:{},getBoundingClientRect:()=>({left:200,top:100,right:700,bottom:800,width:500,height:700}),querySelectorAll:()=>[]};}
function run(id,payload,videoplayer=true){
 let elements=[{type:'img',value:elem('img',image)}];
 if(videoplayer)elements.push({type:'video',value:elem('video','blob:https://www.instagram.com/opaque')});
 const ctx={URL,innerWidth:900,innerHeight:900,location:{pathname:`/stories/alice/${id}/`},
  performance:{getEntriesByType:()=>[]},getComputedStyle:()=>({display:'block',visibility:'visible',opacity:'1'}),
  document:{querySelectorAll:tag=>tag==='script'?[{textContent:JSON.stringify(payload)}]:elements.filter(e=>e.type===tag).map(e=>e.value)}};
 return JSON.parse(vm.runInNewContext(script,ctx));
}
let tests=0;function check(b,desc){assert.ok(b,desc);tests++;}
let obj={reels_media:[{items:[{pk:'999999999',media_type:2,video_versions:[{url:'https://a.cdninstagram.com/wrong.mp4'}]},
 {pk:movieId,media_type:2,image_versions2:{candidates:[{url:image}]},video_versions:[{url:video,width:1080,height:1920}]}]}]};
check(run(movieId,obj).url===video,'correct Story video in nested JSON');
check(run('111111111111111111',obj).error?.includes('video'),'reject other Story video');
check(run(movieId,{items:[{pk:movieId,media_type:2,image_versions2:{candidates:[{url:image}]}}]},false).error?.includes('copertina'),'video metadata without MP4 rejects cover');
check(run(movieId,{items:[{pk:movieId,media_type:2,video_versions:[{url:'https://evil.invalid/m.mp4'}]}]}).error?.includes('video'),'untrusted URL ignored');
check(run(movieId,{items:[{pk:movieId,media_type:1,image_versions2:{candidates:[{url:image,width:1000,height:1000}]}}]},false).type==='image','Story photo can be saved');
console.log('PASS: '+tests+' Story DOM JSON scenarios');
