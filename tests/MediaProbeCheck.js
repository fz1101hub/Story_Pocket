/* Verifica offline dello script iniettato nella WebView (nessun accesso a Instagram). */
const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const path = require('path');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/storypocket/mobile/MainActivity.java'), 'utf8');
const start = source.indexOf('String script =');
const end = source.indexOf(';\n        script = script.replace', start);
assert(start !== -1 && end > start, 'Non riesco a recuperare lo script dalla MainActivity');
const parts = source.substring(start, end).match(/"(?:\\.|[^"\\])*"/g);
const script = parts.map(p => JSON.parse(p)).join('');
new vm.Script(script);
let checks = 0;
function element(type, url, rect, style = {}) {
  const el = { currentSrc: url, src: url, getBoundingClientRect: () => ({
    left: rect[0], top: rect[1], right: rect[0]+rect[2], bottom: rect[1]+rect[3], width:rect[2], height:rect[3]
  }), style, querySelector: () => null };
  return {type,el};
}
const photo = (url, rect=[200,100,500,700], css={}) => element('img',url,rect,css);
const video = (url, rect=[200,100,500,700], css={}) => element('video',url,rect,css);
let resources = [];
function probe(elements) {
  const ctx = {
    URL, performance: {getEntriesByType: () => resources.map(name => ({name}))},
    innerWidth: 900, innerHeight: 900,
    getComputedStyle: el => ({display: el.style.display || 'block',visibility:el.style.visibility || 'visible',opacity:el.style.opacity ?? '1'}),
    document: {querySelectorAll: tag => elements.filter(e=>e.type===tag).map(e=>e.el)}, location: {pathname: '/p/post123/'}
  };
  return JSON.parse(vm.runInNewContext(script, ctx));
}
function eq(actual, expected, label) { assert.deepStrictEqual(actual,expected,label); checks++; }
function yes(value,label) { assert.ok(value,label); checks++; }
const img = 'https://scontent.cdninstagram.com/a.jpg', vid = 'https://video.cdninstagram.com/a.mp4';
eq(probe([]).error,'Nessun media visibile trovato','Pagina vuota');
eq(probe([photo(img)]).url,img,'Immagine visibile');
eq(probe([photo('https://media.bad-example.com/photo.jpg'),photo(img)]).url,img,'Host sconosciuto ignorato');
eq(probe([video(vid)]).type,'video','Video visibile');
eq(probe([photo('http://evil.com/a.jpg')]).error,'Nessun media visibile trovato','HTTP rifiutato');
eq(probe([photo('blob:https://instagram.com/xx')]).error,'Nessun media visibile trovato','Blob rifiutato');
eq(probe([photo(img,[0,0,80,80])]).error,'Nessun media visibile trovato','Miniatura ignorata');
eq(probe([photo(img,[200,100,500,700],{display:'none'})]).error,'Nessun media visibile trovato','Elemento nascosto');
eq(probe([photo(img,[200,100,500,700],{opacity:'0.02'})]).error,'Nessun media visibile trovato','Elemento trasparente');
eq(probe([photo(img,[1000,1000,500,700])]).error,'Nessun media visibile trovato','Fuori dallo schermo');
eq(probe([photo(img),video(vid)]).url,vid,'Video prioritario');
eq(probe([photo('https://cdninstagram.com/small.jpg',[10,10,200,250]),photo(img)]).url,img,'Foto grande preferita');
yes(probe([photo(img)]).score > 0,'Punteggio positivo');
// Video prioritario anche con un player blob, senza salvare la copertina.
const blobVideo = video('blob:https://www.instagram.com/abc');
yes(probe([blobVideo,photo(img)]).error.includes('video'), 'Video blob: foto di copertina rifiutata');
// I Reel non devono MAI salvare l'immagine al posto di un video.
function probeAt(pathname,elements){
 const ctx = {URL,innerWidth:900,innerHeight:900,location:{pathname},getComputedStyle:el=>({display:el.style.display||'block',visibility:el.style.visibility||'visible',opacity:el.style.opacity??'1'}),document:{querySelectorAll:tag=>elements.filter(e=>e.type===tag).map(e=>e.el)}};
 return JSON.parse(vm.runInNewContext(script,ctx));
}
yes(probeAt('/reel/reel123/',[photo(img)]).error.includes('copertina'), 'Reel con solo copertina: errore');
eq(probeAt('/reel/reel123/',[photo(img),video(vid)]).url, vid, 'Reel con video scaricabile');
const meta = {type:'meta',el:{content:vid,getAttribute:k=>k==='property'?'og:video':k==='content'?vid:null}};
eq(probeAt('/reel/reel123/',[photo(img),meta]).url,vid,'Reel con metadato video');
yes(probeAt('/p/post123/',[photo(img),meta]).type==='video', 'Post video: metadato senza fallback immagine');
const blockedVideo = video('');
yes(probeAt('/stories/anna/123/',[photo(img),blockedVideo]).error.includes('video'),'Story video senza URL: no JPEG');
console.log('OK: '+checks+' scenari del selettore multimediale simulato.');

// Original MP4 via network resource history even if the player exposes only blob:.
resources = ['https://scontent.cdninstagram.com/o1/v/t16/f2/m86/original.mp4?stp=dst-mp4'];
eq(probe([blobVideo,photo(img)]).type, 'video', 'Video blob con MP4 dalla rete');
resources = ['https://scontent.cdninstagram.com/cover.jpg'];
yes(probe([blobVideo,photo(img)]).error.includes('video'),'Nessuna conversione JPEG da blob');
resources = ['https://scontent.cdninstagram.com/o1/v/t16/f2/m86/original.mp4?bytestart=0&byteend=512'];
yes(probe([blobVideo,photo(img)]).error.includes('video'),'Segmenti video esclusi');
console.log('OK: '+checks+' scenari inclusa la ricerca del video originale.');
