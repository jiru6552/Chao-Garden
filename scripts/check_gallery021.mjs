#!/usr/bin/env node
// Execute the shipped inline gallery script with a bounded DOM/event adapter.
// This verifies interaction logic and local image resolution, not browser layout.
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import crypto from 'node:crypto';

const root=path.resolve(path.dirname(new URL(import.meta.url).pathname),'..');
const input=path.resolve(process.argv[2]??path.join(root,'docs/release-gallery-0.21.0.html'));
const output=path.resolve(process.argv[3]??path.join(root,'docs/validation-0.21/gallery.json'));
const html=fs.readFileSync(input,'utf8');
const sha=value=>crypto.createHash('sha256').update(value).digest('hex');
let checks=0;
function assert(value,message){if(!value)throw new Error(message);checks++;}
const unescape=value=>value.replace(/&quot;/g,'"').replace(/&#x27;/g,"'").replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&');
class Element {
  constructor(){this.listeners=new Map();this.textContent='';this.value='';this.hidden=false;this.dataset={};this.children={};}
  addEventListener(name,callback){this.listeners.set(name,callback);}
  querySelector(selector){assert(selector in this.children,'Unknown element selector '+selector);return this.children[selector];}
  dispatch(name,event={}){assert(this.listeners.has(name),'Missing event '+name);this.listeners.get(name)({target:this,preventDefault(){},...event});}
}
const raw=[...html.matchAll(/<article class="card" data-category="([^"]+)" data-search="([^"]*)">([\s\S]*?)<\/article>/g)];
const images=[];
const cards=raw.map(match=>{
  const card=new Element();card.dataset={category:unescape(match[1]),search:unescape(match[2])};
  const src=match[3].match(/<img src="([^"]+)" alt="([^"]*)"/);
  const title=match[3].match(/<h2>([\s\S]*?)<\/h2>/);
  const subtitle=match[3].match(/<p class="subtitle">([\s\S]*?)<\/p>/);
  assert(src&&title&&subtitle,'Incomplete gallery card');
  const img=new Element();img.src=unescape(src[1]);img.alt=unescape(src[2]);images.push(img.src);
  const button=new Element();const heading=new Element();heading.textContent=unescape(title[1]);
  const sub=new Element();sub.textContent=unescape(subtitle[1]);
  card.children={img,'button.capture':button,'.capture':button,h2:heading,'.subtitle':sub};
  return card;
});
assert(cards.length===225,'Expected 220 feature and five motion cards');
assert(!/https?:\/\//i.test(html),'Gallery gained a network dependency');
assert(html.includes('[hidden]{display:none!important}'),'Hidden cards must disappear from layout');
for(const src of images){
  if(src.startsWith('data:image/'))assert(src.length>100,'Empty embedded image');
  else assert(fs.statSync(path.resolve(path.dirname(input),src)).size>100,'Missing local image '+src);
}
const search=new Element(),filter=new Element(),count=new Element(),zoom=new Element();
filter.value='all';zoom.open=false;zoom.children={img:new Element(),p:new Element()};
zoom.showModal=()=>{zoom.open=true;};zoom.close=()=>{zoom.open=false;};
const next=new Element(),previous=new Element(),close=new Element();
const elements={'#search':search,'#filter':filter,'#count':count,'#zoom':zoom,'#next':next,'#previous':previous,'#close':close};
const document={querySelector(selector){assert(selector in elements,'Unknown document selector '+selector);return elements[selector];},querySelectorAll(selector){assert(selector==='.card','Unknown collection '+selector);return cards;}};
const scripts=[...html.matchAll(/<script>([\s\S]*?)<\/script>/g)];
assert(scripts.length===1,'Expected the single shipped inline script');
const script=scripts[0][1];new vm.Script(script,{filename:'shipped-gallery-inline.js'}).runInNewContext({document});
const visible=()=>cards.filter(card=>!card.hidden);
const select=(category,query='')=>{filter.value=category;search.value=query;filter.dispatch('change');};
assert(visible().length===225&&count.textContent.startsWith('225 '),'Initial gallery state');
select('companion-live');assert(visible().length===6,'Category filter must show six real companion views');
search.value='SWIM LEVEL 99';search.dispatch('input');assert(visible().length===1,'Search must combine with category, without case sensitivity');
assert(visible()[0].children.h2.textContent.includes('99'),'Search selected wrong companion');
search.value='no-such-view-qxz';search.dispatch('input');assert(visible().length===0&&count.textContent.startsWith('0 '),'Empty search result');
select('adult-endpoints');assert(visible().length===15,'All 15 primary/alignment grids must remain reachable');
const list=visible();list[0].children['.capture'].dispatch('click');
assert(zoom.open&&zoom.children.img.src===list[0].children.img.src,'Open enlarged full capture');
next.dispatch('click');assert(zoom.children.img.src===list[1].children.img.src,'Next visible result');
previous.dispatch('click');assert(zoom.children.img.src===list[0].children.img.src,'Previous visible result');
previous.dispatch('click');assert(zoom.children.img.src===list.at(-1).children.img.src,'Previous wraps within visible results');
zoom.dispatch('keydown',{key:'ArrowRight'});assert(zoom.children.img.src===list[0].children.img.src,'Arrow-key next wraps');
zoom.dispatch('keydown',{key:'ArrowLeft'});assert(zoom.children.img.src===list.at(-1).children.img.src,'Arrow-key previous wraps');
assert(zoom.children.p.textContent.includes(list.at(-1).children.h2.textContent),'Enlarged caption follows image');
close.dispatch('click');assert(!zoom.open,'Close button');
list[0].children['.capture'].dispatch('click');zoom.dispatch('click');assert(!zoom.open,'Dialog backdrop close');
select('all');assert(visible().length===225,'Restore complete gallery');
const report={release:'0.21.0',result:'PASS',method:'Exact shipped inline JavaScript executed in Node VM with bounded DOM/event adapter; local image files checked. No graphical browser engine was available, so this is not a browser-layout claim.',checks,cards:cards.length,static_cards:220,motion_cards:5,local_images_resolved:images.filter(s=>!s.startsWith('data:')).length,embedded_images:images.filter(s=>s.startsWith('data:')).length,html:path.relative(root,input),html_sha256:sha(html),inline_script_sha256:sha(script),interactions:['initial open','category filter','case-insensitive search combined with category','empty results','restore all','enlarge full capture','next/previous visible result','cyclic wrap','left/right arrows','caption update','close button','dialog backdrop close']};
fs.mkdirSync(path.dirname(output),{recursive:true});fs.writeFileSync(output,JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
