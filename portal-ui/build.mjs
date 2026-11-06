import {build} from 'esbuild';
import * as sass from 'sass';
import {readFile,mkdir,writeFile,copyFile,readdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
const root=path.dirname(fileURLToPath(import.meta.url));
const output=path.resolve(root,'../app/src/main/assets/carbon');
await mkdir(output,{recursive:true});
const css=sass.compile(path.join(root,'src/styles.scss'),{loadPaths:[path.join(root,'node_modules')],style:'compressed',silenceDeprecations:['legacy-js-api','import','global-builtin','color-functions'],quietDeps:true}).css;
const charts=await readFile(path.join(root,'node_modules/@carbon/charts/dist/styles.min.css'),'utf8');
await writeFile(path.join(output,'vendor.css'),css+'\n'+charts);
const built=await build({entryPoints:[path.join(root,'src/index.js')],outfile:path.join(output,'vendor.js'),bundle:true,minify:true,format:'iife',target:['es2022'],loader:{'.svg':'text'},legalComments:'eof',metafile:true});
for(const pkg of ['styles','charts','web-components']){
 const dir=path.resolve(root,'../third_party/carbon-'+pkg);await mkdir(dir,{recursive:true});
 const filename=pkg==='charts'?'LICENSE.md':'LICENSE';await copyFile(path.join(root,'node_modules/@carbon',pkg,filename),path.join(dir,'LICENSE'));
 const data=JSON.parse(await readFile(path.join(root,'node_modules/@carbon',pkg,'package.json'),'utf8'));await writeFile(path.join(dir,'SOURCE.json'),JSON.stringify({package:data.name,version:data.version},null,2));
}
const notices=new Map();
for(const input of Object.keys(built.metafile.inputs)){
 let directory=path.dirname(path.resolve(root,input));
 while(directory.startsWith(root)&&directory!==root){
  try{
   const data=JSON.parse(await readFile(path.join(directory,'package.json'),'utf8'));
   if(data.name&&data.version){
    const names=await readdir(directory);const license=names.find(name=>/^licen[cs]e(?:\.(?:md|txt))?$/i.test(name));
    if(license)notices.set(data.name+'@'+data.version,await readFile(path.join(directory,license),'utf8'));
    break;
   }
  }catch{}
  directory=path.dirname(directory);
 }
}
const noticeDirectory=path.resolve(root,'../third_party/portal-dependencies');await mkdir(noticeDirectory,{recursive:true});
await writeFile(path.join(noticeDirectory,'NOTICES.txt'),[...notices].sort(([a],[b])=>a.localeCompare(b)).map(([name,license])=>name+'\n'+'='.repeat(name.length)+'\n'+license).join('\n\n'));
const generated=spawnSync(process.platform==='win32'?'python':'python3',[path.join(root,'generate-icons.py')],{stdio:'inherit'});if(generated.status!==0)process.exit(generated.status||1);
console.log('Bundled Carbon styles, web components, icons and charts for offline use.');
