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
 return JSON.parse(vm.runInNewContext(script,c¶»§q«^