import '@carbon/web-components/es/components/notification/inline-notification.js';
import '@carbon/web-components/es/components/breadcrumb/index.js';
import '@carbon/web-components/es/components/tag/tag.js';
import {DonutChart,SimpleBarChart} from '@carbon/charts';
import dashboard from '@carbon/icons/svg/32/dashboard.svg';
import building from '@carbon/icons/svg/32/building.svg';
import document from '@carbon/icons/svg/32/document.svg';
import ruler from '@carbon/icons/svg/32/ruler.svg';
import users from '@carbon/icons/svg/32/user--multiple.svg';
import arrow from '@carbon/icons/svg/32/arrow--right.svg';
import search from '@carbon/icons/svg/32/search.svg';
import download from '@carbon/icons/svg/32/download.svg';
import sort from '@carbon/icons/svg/32/arrows--vertical.svg';
import chevron from '@carbon/icons/svg/32/chevron--down.svg';

function icon(source,extra='') {
 const template=window.document.createElement('template');template.innerHTML=source;
 const svg=template.content.firstElementChild;svg.setAttribute('class','carbon-icon '+extra);svg.setAttribute('aria-hidden','true');svg.setAttribute('focusable','false');return svg;
}
function enhanceForms() {
 for(const button of window.document.querySelectorAll('button:not(.carbon-sort)')){
  button.classList.add('cds--btn',button.classList.contains('danger')?'cds--btn--danger':'cds--btn--primary');
  if(!button.querySelector('svg')){
   const label=button.textContent.toLowerCase();
   button.prepend(icon(label.includes('download')?download:label.includes('upload')||label.includes('restore')||label.includes('replace')?download:arrow));
  }
 }
 for(const input of window.document.querySelectorAll('input:not([type=hidden]):not([type=checkbox]):not([type=file]):not([type=color]),textarea'))input.classList.add('cds--text-input');
 for(const label of window.document.querySelectorAll('form label'))label.classList.add('cds--label');
 for(const select of window.document.querySelectorAll('form select')){
  select.classList.add('cds--select-input');const wrap=window.document.createElement('div');wrap.className='cds--select';select.before(wrap);wrap.append(select,icon(chevron,'cds--select__arrow'));
 }
 let id=0;
 for(const input of window.document.querySelectorAll('form input[type=checkbox]')){
  const label=input.parentElement;if(label.tagName!=='LABEL')continue;
  input.id ||= 'carbon-checkbox-'+(++id);input.classList.add('cds--checkbox');label.className='cds--checkbox-label';label.htmlFor=input.id;
  const wrap=window.document.createElement('div');wrap.className='cds--form-item cds--checkbox-wrapper';label.before(wrap);wrap.append(input,label);
 }
}
function enhanceFormBehavior() {
 let dirty=false,submitting=false;
 for(const form of window.document.querySelectorAll('form')){
  const mutating=form.method.toLowerCase()==='post';
  form.addEventListener('input',event=>{if(mutating)dirty=true;event.target.removeAttribute('aria-invalid');});
  form.addEventListener('change',()=>{if(mutating)dirty=true;});
  const feedback=window.document.createElement('p');feedback.setAttribute('role','status');feedback.className='carbon-form-feedback';form.append(feedback);
  feedback.id='form-feedback-'+Math.random().toString(36).slice(2);
  form.addEventListener('invalid',event=>{event.target.setAttribute('aria-describedby',feedback.id);event.target.setAttribute('aria-invalid','true');feedback.textContent='Check the highlighted field: '+(event.target.labels?.[0]?.textContent.trim()||event.target.name)+'. '+event.target.validationMessage;},true);
  form.addEventListener('submit',event=>{
   if(new URL(form.action,location.href).pathname==='/workspace/export'){feedback.textContent='Download requested. Check your browser downloads; exporting does not issue or save the document.';return;}
   if(form.dataset.saving==='true'){event.preventDefault();return;}
   if(!form.checkValidity()){event.preventDefault();form.reportValidity();return;}
   form.dataset.saving='true';form.setAttribute('aria-busy','true');submitting=true;dirty=false;
   feedback.textContent='Saving. Please wait for the result before submitting again.';
   // Keep the controls and submitter enabled until serialization; guard handles repeated events.
   for(const button of form.querySelectorAll('button[type=submit],button:not([type])'))button.setAttribute('aria-disabled','true');
  });
 }
 window.addEventListener('beforeunload',event=>{if(dirty&&!submitting){event.preventDefault();event.returnValue='';}});
 window.addEventListener('pageshow',()=>{submitting=false;for(const form of window.document.querySelectorAll('form')){delete form.dataset.saving;form.removeAttribute('aria-busy');form.querySelectorAll('[aria-disabled]').forEach(b=>b.removeAttribute('aria-disabled'));}});
}
function iconForLabel(name){
 const text=name.toLowerCase();
 if(text.includes('overview')||text.includes('dashboard'))return dashboard;
 if(text.includes('client')||text.includes('contractor')||text.includes('contact'))return users;
 if(text.includes('item')||text.includes('measure')||text.includes('ruler')||text.includes('assigned'))return ruler;
 if(text.includes('company')||text.includes('project'))return building;
 if(text.includes('document')||text.includes('letter')||text.includes('export'))return document;
 if(text.includes('transfer')||text.includes('backup'))return download;
 return document;
}
function enhanceModernShell(){
 const selected=new URLSearchParams(location.search).get('section')||'';
 for(const link of window.document.querySelectorAll('.cds--side-nav__link, .cds--header__menu-item, .portal-workflow a')){
  const target=new URL(link.href,location.href);
  if(target.pathname==='/workspace'&&(target.searchParams.get('section')||'dashboard')===selected)link.setAttribute('aria-current','page');
  if(!link.querySelector('svg'))link.prepend(icon(iconForLabel(link.textContent)));
 }
 for(const tile of window.document.querySelectorAll('a.cds--tile--clickable')){
  if(tile.querySelector('.portal-tile-icon, .carbon-icon'))continue;
  const glyph=icon(iconForLabel(tile.textContent),'portal-tile-glyph');
  const wrap=window.document.createElement('span');wrap.className='portal-tile-icon';wrap.append(glyph);
  tile.classList.add('portal-tile');tile.prepend(wrap);
  if(!tile.querySelector('.portal-tile-arrow'))tile.append(icon(arrow,'portal-tile-arrow'));
 }
}
function enhanceShell() {
 if(window.document.querySelector('.cds--header')){enhanceModernShell();return;}
 const main=window.document.querySelector('main');if(!main)return;
 const header=window.document.createElement('header');header.className='carbon-shell';header.setAttribute('aria-label','ArchiMan');
 const brand=window.document.createElement('a');brand.href='/workspace?section=dashboard';brand.append(icon(dashboard),window.document.createTextNode('ArchiMan'));
 const section=window.document.createElement('span');section.className='carbon-shell-section';section.textContent='Architecture practice';header.append(brand,section);const skip=window.document.querySelector('a[href="#content"]');if(skip){skip.classList.add('carbon-skip');skip.after(header);}else window.document.body.prepend(header);
 const heading=main.querySelector('h1');if(heading){const crumbs=window.document.createElement('cds-breadcrumb');crumbs.setAttribute('aria-label','Breadcrumb');
  const item=window.document.createElement('cds-breadcrumb-item');const link=window.document.createElement('cds-breadcrumb-link');link.href='/workspace?section=dashboard';link.textContent='Practice';item.append(link);
  const current=window.document.createElement('cds-breadcrumb-item');current.textContent=heading.textContent;current.setAttribute('aria-current','page');crumbs.append(item,current);heading.before(crumbs);
 }
 const selected=new URLSearchParams(location.search).get('section')||'dashboard';
 for(const link of main.querySelectorAll('nav a')){
  const target=new URL(link.href,location.href);if(target.pathname==='/workspace'&&(target.searchParams.get('section')||'dashboard')===selected)link.setAttribute('aria-current','page');
  link.prepend(icon(iconForLabel(link.textContent)));
 }
}
function enhanceNotifications() {
 for(const element of window.document.querySelectorAll('[role=status],[role=alert],p.notice')){
  if(!element.textContent.trim()||element.childElementCount)continue;
  const notice=window.document.createElement('cds-inline-notification');const requested=element.dataset.kind;notice.kind=element.getAttribute('role')==='alert'?'error':['success','warning','error','info'].includes(requested)?requested:'info';notice.title=notice.kind==='error'?'Check your entries':notice.kind==='success'?'Saved':notice.kind==='warning'?'Review needed':'Update';notice.subtitle=element.textContent;notice.hideCloseButton=true;notice.lowContrast=true;element.replaceWith(notice);
 }
 for(const node of window.document.querySelectorAll('.status')){
  const tag=window.document.createElement('cds-tag');tag.textContent=node.textContent;tag.type=/approved|issued/.test(node.className)?'green':/returned|void/.test(node.className)?'red':/checked/.test(node.className)?'warm-gray':'blue';tag.size='sm';node.replaceWith(tag);
 }
}
function enhanceTables() {
 const collator=new Intl.Collator(undefined,{numeric:true,sensitivity:'base'});
 const compare=(first,second)=>{const number=value=>/^(?:INR\s*|₹\s*)?-?[0-9,]+(?:\.[0-9]+)?$/.test(value.trim())?Number(value.replace(/INR|₹|,/g,'').trim()):NaN;const a=number(first),b=number(second);return Number.isFinite(a)&&Number.isFinite(b)?a-b:collator.compare(first,second);};
 for(const [index,table] of [...window.document.querySelectorAll('table')].entries()){
  if(table.closest('.carbon-insight'))continue;
  const body=table.tBodies[0];if(!body)continue;const rows=[...body.rows];let filtered=rows,page=0,size=25,column=-1,direction=1;
  table.classList.add('cds--data-table');table.id ||= 'carbon-table-'+index;
  const headings=[...window.document.querySelectorAll('h1,h2,h3')].filter(h=>h.compareDocumentPosition(table)&Node.DOCUMENT_POSITION_FOLLOWING);
  const tableName=table.caption?.textContent.trim()||headings.at(-1)?.textContent.trim()||'Records';
  if(!table.caption){const caption=table.createCaption();caption.textContent=tableName;caption.className='carbon-table-caption';}

  const host=table.closest('.tablewrap')||table.parentElement;
  const toolbar=window.document.createElement('div');toolbar.className='carbon-table-toolbar';toolbar.setAttribute('role','search');
  const input=window.document.createElement('input');input.type='search';input.className='cds--text-input';input.placeholder='Search this table';input.setAttribute('aria-label','Search '+tableName);input.setAttribute('aria-controls',table.id);toolbar.append(icon(search),input);host.insertBefore(toolbar,table);
  const pages=window.document.createElement('div');pages.className='carbon-table-pages';
  const pageLabel=window.document.createElement('label');pageLabel.textContent='Rows per page';const perPage=window.document.createElement('select');perPage.setAttribute('aria-label','Rows per page');
  for(const value of [25,50,100]){const option=window.document.createElement('option');option.value=value;option.textContent=value;perPage.append(option);}pageLabel.append(perPage);
  const status=window.document.createElement('span');status.setAttribute('aria-live','polite');
  const previous=window.document.createElement('button');previous.type='button';previous.textContent='Previous';previous.className='cds--btn cds--btn--ghost';
  const next=window.document.createElement('button');next.type='button';next.textContent='Next';next.className='cds--btn cds--btn--ghost';pages.append(pageLabel,status,previous,next);host.append(pages);
  const render=()=>{page=Math.max(0,Math.min(page,Math.max(0,Math.ceil(filtered.length/size)-1)));rows.forEach(row=>row.hidden=true);filtered.slice(page*size,(page+1)*size).forEach(row=>row.hidden=false);previous.disabled=page===0;next.disabled=(page+1)*size>=filtered.length;status.textContent=filtered.length?`${page*size+1}–${Math.min((page+1)*size,filtered.length)} of ${filtered.length}`:'No matching rows';};
  input.addEventListener('input',()=>{const query=input.value.trim().toLocaleLowerCase();filtered=rows.filter(row=>row.textContent.toLocaleLowerCase().includes(query));if(column>=0)filtered.sort((a,b)=>direction*compare(a.cells[column]?.textContent||'',b.cells[column]?.textContent||''));filtered.forEach(row=>body.append(row));page=0;render();});
  perPage.addEventListener('change',()=>{size=Number(perPage.value);page=0;render();});previous.addEventListener('click',()=>{page--;render();});next.addEventListener('click',()=>{page++;render();});
  for(const [cellIndex,cell] of [...(table.tHead?.rows[0]?.cells||[])].entries()){
   cell.scope='col';const button=window.document.createElement('button');button.type='button';button.className='carbon-sort';button.append(window.document.createTextNode(cell.textContent),icon(sort));cell.replaceChildren(button);
   button.addEventListener('click',()=>{direction=column===cellIndex?-direction:1;column=cellIndex;for(const header of table.tHead.rows[0].cells)header.removeAttribute('aria-sort');cell.setAttribute('aria-sort',direction===1?'ascending':'descending');filtered.sort((a,b)=>direction*compare(a.cells[column]?.textContent||'',b.cells[column]?.textContent||''));filtered.forEach(row=>body.append(row));page=0;render();});
  }
  render();
 }
}
function enhanceCharts() {
 const charts=[];
 const theme=()=>window.document.documentElement.dataset.theme==='dark'?'g100':'g10';
 const palette=()=>theme()==='g100'?['#be95ff','#33b1ff','#3ddbd9','#ff7eb6','#a6c8ff']:['#6929c4','#1192e8','#005d5d','#9f1853','#002d9c'];
 for(const element of window.document.querySelectorAll('[data-carbon-chart]')){
  try{
   const data=JSON.parse(element.dataset.rows);if(!Array.isArray(data)||!data.length||!data.some(row=>Number(row.value)>0)){element.className='carbon-empty';element.textContent='No data to chart yet.';continue;}
   const safe=data.map(row=>({group:String(row.group),value:Number(row.value)}));if(safe.some(row=>!Number.isFinite(row.value)||row.value<0))throw new Error('Invalid chart value');
   const options={height:'320px',resizable:true,theme:theme(),animations:false,toolbar:{enabled:false},tooltip:{enabled:false},legend:{enabled:true},color:{scale:Object.fromEntries(safe.map((row,i)=>[row.group,palette()[i%palette().length]]))}};
   const chart=element.dataset.carbonChart==='donut'?new DonutChart(element,{data:safe,options:{...options,donut:{center:{label:'Projects'}},pie:{labels:{enabled:false}}}}):new SimpleBarChart(element,{data:safe,options:{...options,axes:{left:{mapsTo:'group',scaleType:'labels'},bottom:{mapsTo:'value',title:'INR',includeZero:true}},bars:{maxWidth:32}}});
   charts.push({chart,groups:safe.map(row=>row.group)});
  }catch(error){element.className='carbon-empty';element.textContent='Chart unavailable. Exact values are listed below.';}
 }
 const observer=new MutationObserver(()=>{for(const {chart,groups} of charts)chart.model.setOptions({theme:theme(),color:{scale:Object.fromEntries(groups.map((group,i)=>[group,palette()[i%palette().length]]))}});});observer.observe(window.document.documentElement,{attributes:true,attributeFilter:['data-theme']});
}
function start(){enhanceShell();enhanceForms();enhanceFormBehavior();enhanceNotifications();enhanceTables();enhanceCharts();}
if(window.document.readyState==='loading')window.document.addEventListener('DOMContentLoaded',start);else start();
