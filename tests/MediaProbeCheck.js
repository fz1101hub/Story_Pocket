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
    getComputedStyle: el => ({display: el.style.display || 'block',visibility:el.style.visibility || 'visible',opacity:el.style.opacity¶»§q«^