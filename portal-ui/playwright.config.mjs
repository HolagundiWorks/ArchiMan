import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./tests',workers:1,timeout:30000,use:{channel:process.platform==='win32'?'msedge':undefined,headless:true,viewport:{width:1280,height:900}},reporter:'list',outputDir:'../app/build/audit/portal-browser-tests'});
