package com.holagundi.archiman.portal
object PortalWorkspaceAssets {
    val css = """
    [hidden]{display:none!important}
    .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}
    .dimensions{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.5rem}
    input[type=checkbox]{inline-size:auto;accent-color:var(--cds-border-interactive, var(--button-primary))}
    input[type=color]{block-size:3rem}
    output{display:block;padding:1rem;background:var(--cds-layer-selected, var(--highlight));color:var(--cds-text-primary, var(--highlight-text));font-weight:600}
    .tablewrap{overflow:auto}
    .paper{background:#ffffff;color:#161616;color-scheme:light;min-height:650px;max-width:50rem;margin:1.5rem auto;padding:2.5rem;--accent:#0f62fe}
    .paper hr{border:1px solid var(--accent)}
    .logo{width:5.5rem;height:5.5rem;object-fit:contain;float:right}
    .paper p{margin:.25rem 0}
    .paper footer{margin-top:12rem;font-size:.75rem}
    article{margin-block:1rem}
    @media(max-width:600px){.grid,.dimensions{grid-template-columns:1fr}}
    @media print{.portal-workflow,form{display:none}.paper{border:0;margin:0}}
    .portal-canvas form > label { display: grid; gap: 0.25rem; margin: 0.75rem 0; font-size: 0.875rem; }
    .portal-canvas form > label > input, .portal-canvas form > label > select, .portal-canvas form > label > textarea {
      width: 100%; min-height: 2.5rem; padding: 0.5rem 0.75rem; box-sizing: border-box;
      border: 1px solid #8d8d8d; background: transparent; color: inherit; font: inherit;
    }
    .portal-canvas form > button {
      min-height: 2.5rem; padding: 0 1rem; margin-top: 0.5rem; border: 0; cursor: pointer;
      background: #0f62fe; color: #fff; font: inherit;
    }
    .portal-canvas form > button:disabled { opacity: 0.5; cursor: not-allowed; }
    .portal-canvas article form > button { min-height: 2rem; padding: 0 0.75rem; background: transparent; color: #0f62fe; text-decoration: underline; }
    .portal-canvas article form { display: inline; }
    .portal-canvas article form > label { display: flex; align-items: center; gap: 0.5rem; }
    .portal-canvas article form > label > input { width: auto; min-height: 0; }
  """.trimIndent()
    val js = """
    'use strict';
    document.querySelectorAll('input[type=number]').forEach(input=>{input.step='any';input.min='0';input.inputMode='decimal';});
    const logo=document.getElementById('logo-file');
    if(logo) logo.addEventListener('change',()=>{const file=logo.files[0];if(!file)return;if(file.size>524288||!['image/png','image/jpeg'].includes(file.type)){alert('Choose PNG/JPEG, maximum 512 KB');logo.value='';return;}const reader=new FileReader();reader.onload=()=>{document.querySelector('input[name=logo]').value=reader.result;};reader.readAsDataURL(file);});
    const csv=document.getElementById('csv-file');
    if(csv) csv.addEventListener('change',()=>{const file=csv.files[0];if(!file)return;if(file.size>1000000){alert('CSV maximum 1 MB');return;}const reader=new FileReader();reader.onload=()=>{document.querySelector('textarea[name=csv]').value=reader.result;};reader.readAsText(file);});
    const item=document.getElementById('work-item');
    if(item){const form=item.closest('form');let previous=item.value;const description=form.elements.namedItem('description');let custom=false;if(description)description.addEventListener('input',()=>{custom=true;});item.addEventListener('change',()=>{const dimensions=['length','breadth','height','deduction'].map(key=>form.elements.namedItem(key)).filter(Boolean);if(dimensions.some(input=>input.value!==''&&Number(input.value)!==0)&&!confirm('Change work item and clear dimensions? This prevents using measurements with the wrong formula.')){item.value=previous;return;}dimensions.forEach(input=>input.value='');previous=item.value;if(description&&!custom)description.value=item.selectedOptions[0]?.textContent||'';});}

    if(item){const form=item.closest('form');const update=()=>{const option=item.options[item.selectedIndex];if(!option)return;const type=option.dataset.formula;const imperial=form.elements.namedItem('system').value==='IMPERIAL';const dimension=imperial?'ft':'m';const n=key=>{const value=Number(form.elements.namedItem(key).value||0);return Number.isFinite(value)&&value>=0?value:NaN;};let q=n('nos');if(type!=='NOS')q*=n('length');if(type==='AREA'||type==='VOLUME')q*=n('breadth');if(type==='VOLUME'||type==='WALL_PLASTER')q*=n('height');if(type==='WALL_PLASTER')q-=n('deduction');const power=type==='VOLUME'?3:(type==='AREA'||type==='WALL_PLASTER'?2:(type==='NOS'?0:1));const unit=power===0?'Nos':dimension+(power===3?'³':power===2?'²':'');const savedUnit=option.dataset.unit;const savedImperial=savedUnit.includes('ft');const saved=Math.max(0,q)*(power===0||imperial===savedImperial?1:Math.pow(3.280839895013123,imperial?-power:power));document.getElementById('quantity').textContent='Calculated quantity: '+(Number.isFinite(q)?Math.max(0,q).toFixed(2):'Invalid dimensions')+' '+unit+' | Saved '+(Number.isFinite(saved)?saved.toFixed(2):'—')+' '+savedUnit;for(const key of ['length','breadth','height','deduction']){const show=key==='length'?type!=='NOS':key==='breadth'?['AREA','VOLUME'].includes(type):key==='height'?['VOLUME','WALL_PLASTER'].includes(type):type==='WALL_PLASTER';form.elements.namedItem(key).parentElement.hidden=!show;} };form.addEventListener('input',update);let priorSystem=form.elements.namedItem('system').value;form.addEventListener('change',event=>{if(event.target===form.elements.namedItem('system')){const next=form.elements.namedItem('system').value;if(next!==priorSystem){const factor=next==='IMPERIAL'?3.280839895013123:1/3.280839895013123;for(const key of ['length','breadth','height','deduction']){const input=form.elements.namedItem(key);if(input.value!=='')input.value=(Number(input.value)*Math.pow(factor,key==='deduction'?2:1)).toFixed(8).replace(/0+$/,'').replace(/\.$/,'');}priorSystem=next;}}update();});update();}

    (function(){
      const product=document.querySelector('[data-portal-product]');
      if(product){
        const tabs=[...product.querySelectorAll('.portal-product-tab')];
        const panels=[...product.querySelectorAll('.portal-product-panel-host')];
        const activate=panel=>{
          tabs.forEach(tab=>{
            const on=tab.dataset.panel===panel;
            tab.setAttribute('aria-selected', on?'true':'false');
          });
          panels.forEach(section=>{
            const on=section.dataset.panel===panel;
            section.classList.toggle('is-active', on);
            section.hidden=!on;
          });
        };
        tabs.forEach(tab=>tab.addEventListener('click',()=>activate(tab.dataset.panel)));
      }
    })();

    (function(){
      const board=document.querySelector('.portal-kanban');
      if(!board) return;
      const csrf=board.dataset.csrf||'';
      let dragId=null, dragProject=null;
      const tabs=[...board.querySelectorAll('.portal-kanban-tab')];
      const panels=[...board.querySelectorAll('.portal-kanban-column')];
      const activate=status=>{
        tabs.forEach(tab=>{
          const on=tab.dataset.status===status;
          tab.setAttribute('aria-selected', on?'true':'false');
        });
        panels.forEach(panel=>{
          const on=panel.dataset.status===status;
          panel.classList.toggle('is-active', on);
          panel.hidden=!on;
        });
      };
      tabs.forEach(tab=>tab.addEventListener('click',()=>activate(tab.dataset.status)));
      board.querySelectorAll('.portal-kanban-card').forEach(card=>{
        card.addEventListener('dragstart',event=>{
          dragId=card.dataset.taskId;
          dragProject=card.dataset.projectId;
          event.dataTransfer.setData('text/plain', dragId);
          event.dataTransfer.effectAllowed='move';
        });
        card.addEventListener('dragend',()=>{dragId=null;dragProject=null;panels.forEach(col=>col.classList.remove('is-drop'));});
      });
      panels.forEach(column=>{
        column.addEventListener('dragover',event=>{event.preventDefault();event.dataTransfer.dropEffect='move';column.classList.add('is-drop');});
        column.addEventListener('dragleave',()=>column.classList.remove('is-drop'));
        column.addEventListener('drop',event=>{
          event.preventDefault();
          column.classList.remove('is-drop');
          const id=dragId||event.dataTransfer.getData('text/plain');
          const project=dragProject||column.dataset.projectId;
          const status=column.dataset.status;
          if(!id||!project||!status) return;
          const form=document.createElement('form');
          form.method='post'; form.action='/workspace';
          const fields={csrf,action:'taskMove',requestId:crypto.randomUUID?crypto.randomUUID():String(Date.now()),project,id,status};
          Object.keys(fields).forEach(key=>{const input=document.createElement('input');input.type='hidden';input.name=key;input.value=fields[key];form.appendChild(input);});
          document.body.appendChild(form); form.submit();
        });
      });
    })();
    """.trimIndent()
}
