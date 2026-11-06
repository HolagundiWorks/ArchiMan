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
    """.trimIndent()
}
