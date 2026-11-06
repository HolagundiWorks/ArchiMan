import {test,expect} from '@playwright/test';
import http from 'node:http';
import {readFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
let server,origin;
const rows=Array.from({length:60},(_,i)=>`<tr><td>Project ${i+1}</td><td>${i===0?'1,000.00':i===1?'99.00':String(i+1000)+'.00'}</td></tr>`).join('');
const fixture=`<!doctype html><html><head><meta charset=utf-8><link rel=stylesheet href=/theme.css><link rel=stylesheet href=/carbon/vendor.css><link rel=stylesheet href=/workspace.css><script src=/theme.js></script><script src=/carbon/vendor.js defer></script></head><body><a href=#content>Skip to content</a><main id=content><h1>Practice overview</h1><p role=status data-kind=success>Saved safely.</p><form id=entry><label>Project<input name=project value="Project 1"></label><label>Unit<select name=unit><option value=m>Metric</option></select></label><label><input name=reviewed type=checkbox value=yes>Reviewed quantities</label><button type=submit>Save entry</button></form><div class=carbon-insights><section class=carbon-insight><h2>Project mix</h2><div class=carbon-chart data-carbon-chart=donut data-rows='[{"group":"Active","value":2},{"group":"Archived","value":1}]'></div></section><section class=carbon-insight><h2>Fee balances</h2><div class=carbon-chart data-carbon-chart=bar data-rows='[{"group":"Cash received","value":900},{"group":"TDS withheld","value":100},{"group":"Outstanding","value":0}]'></div></section></div><div class=tablewrap><table><caption>Project rates</caption><thead><tr><th>Project</th><th>Rate</th></tr></thead><tbody>${rows}</tbody></table></div></main></body></html>`;
test.beforeAll(async()=>{
 server=http.createServer(async(req,res)=>{
  try{
   const url=new URL(req.url,'http://localhost');let file;
   if(url.pathname.startsWith('/carbon/'))file=path.join(root,'app/src/main/assets',url.pathname);
   if(['/theme.css','/theme.js','/workspace.css','/workspace.js'].includes(url.pathname))file=path.join(root,'app/build/audit',url.pathname.slice(1));
   res.setHeader('Content-Security-Policy',"default-src 'none'; style-src 'self' 'unsafe-inline'; script-src 'self'; font-src 'self'; img-src 'self' data:; form-action 'self'; base-uri 'none'");
   res.setHeader('Content-Type',file?(file.endsWith('.js')?'application/javascript':file.endsWith('.css')?'text/css':'font/ttf'):'text/html');
   res.end(file?await readFile(file):fixture);
  }catch{res.statusCode=404;res.end('Not found');}
 });await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));origin=`http://127.0.0.1:${server.address().port}`;
});
test.beforeEach(async({page})=>{page.on('dialog',dialog=>dialog.accept());});
test.afterAll(async()=>{await new Promise(resolve=>server.close(resolve));});
test('official components, fonts, charts and form values work under the portal CSP',async({page})=>{
 const errors=[];page.on('pageerror',error=>errors.push(error.message));
 await page.goto(origin);await page.waitForFunction(()=>!!document.querySelector('cds-inline-notification')?.shadowRoot);
 await expect(page.locator('[data-carbon-chart=donut]').getByText('Active',{exact:true}).first()).toBeVisible();
 await expect(page.locator('[data-carbon-chart=bar]').getByText('Cash received',{exact:true}).first()).toBeVisible();
 await expect(page.getByText('Chart unavailable. Exact values are listed below.')).toHaveCount(0);
 expect(await page.getByRole('button',{name:'Save entry'}).evaluate(el=>el.getBoundingClientRect().height)).toBeGreaterThanOrEqual(48);
 expect(await page.evaluate(()=>document.fonts.ready.then(()=>document.fonts.check('14px "IBM Plex Sans"')))).toBe(true);
 await page.getByText('Reviewed quantities',{exact:true}).click();
 expect(await page.locator('#entry').evaluate(form=>Object.fromEntries(new FormData(form)))).toEqual({project:'Project 1',unit:'m',reviewed:'yes'});
 await page.screenshot({path:path.join(root,'app/build/audit/carbon-portal-light.png'),fullPage:true});
 await page.getByRole('combobox',{name:'Color theme'}).selectOption('dark');
 await expect(page.locator('body')).toHaveCSS('background-color','rgb(22, 22, 22)');
 expect(await page.locator('cds-inline-notification').evaluate(el=>getComputedStyle(el).getPropertyValue('--cds-notification-background-info').trim())).toBe('#001d6c');
 await page.screenshot({path:path.join(root,'app/build/audit/carbon-portal-dark.png'),fullPage:true});
 await page.reload();await expect(page.locator('html')).toHaveAttribute('data-theme','dark');
 expect(errors).toEqual([]);
});
test('table search, numeric sorting, pagination and keyboard controls retain rows',async({page})=>{
 await page.goto(origin);const body=page.locator('tbody');await expect(body.locator('tr:visible')).toHaveCount(25);
 await page.getByRole('button',{name:'Next',exact:true}).click();await expect(page.getByText('26–50 of 60',{exact:true})).toBeVisible();
 const search=page.getByRole('searchbox',{name:'Search Project rates'});await search.fill('Project 60');await expect(body.locator('tr:visible')).toHaveCount(1);await expect(body.locator('tr:visible')).toContainText('Project 60');
 await search.fill('');await page.getByRole('button',{name:'Rate',exact:true}).click();await expect(body.locator('tr:visible').first()).toContainText('99.00');
 await expect(page.getByRole('columnheader',{name:'Rate'})).toHaveAttribute('aria-sort','ascending');
 await page.getByRole('combobox',{name:'Rows per page'}).selectOption('100');await expect(body.locator('tr:visible')).toHaveCount(60);await expect(body.locator('tr')).toHaveCount(60);
 await search.fill('no such project');await expect(page.getByText('No matching rows',{exact:true})).toBeVisible();
});

test('typed feedback, named tables and narrow layout preserve accessible controls',async({page})=>{
 await page.setViewportSize({width:320,height:800});await page.goto(origin);
 await expect(page.locator('cds-inline-notification')).toHaveAttribute('kind','success');
 await expect(page.getByRole('searchbox',{name:'Search Project rates'})).toBeVisible();
 await expect(page.locator('caption')).toContainText('Project rates');
 await page.locator('[name=project]').fill('Unsaved project');
 await page.reload();
 await expect(page.getByRole('button',{name:'Save entry'})).toBeVisible();
 const bounds=await page.getByRole('button',{name:'Save entry'}).boundingBox();expect(bounds.x+bounds.width).toBeLessThanOrEqual(320);
});

test('invalid forms explain the field and repeat submission is guarded',async({page})=>{
 await page.goto(origin);const field=page.locator('[name=project]');await field.evaluate(input=>input.required=true);await field.fill('');
 await page.getByRole('button',{name:'Save entry'}).click();
 await expect(field).toHaveAttribute('aria-invalid','true');await expect(page.locator('.carbon-form-feedback')).toContainText('Check the highlighted field');
 await field.fill('Project 1');
 // Exercise submit event guards without leaving the test document.
 const result=await page.locator('#entry').evaluate(form=>{const first=new Event('submit',{bubbles:true,cancelable:true});form.dispatchEvent(first);const second=new Event('submit',{bubbles:true,cancelable:true});form.dispatchEvent(second);return {first:first.defaultPrevented,second:second.defaultPrevented,busy:form.getAttribute('aria-busy')};});
 expect(result).toEqual({first:false,second:true,busy:'true'});await expect(page.locator('.carbon-form-feedback')).toContainText('Saving.');
});
