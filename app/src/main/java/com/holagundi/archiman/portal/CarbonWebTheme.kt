package com.holagundi.archiman.portal

/** Shared by the sign-in/project pages and the administrative workspace. */
object CarbonWebTheme {
    private val light = """
        color-scheme:light;--background:#f4f4f4;--layer:#ffffff;--layer-hover:#e8e8e8;--field:#ffffff;
        --text:#161616;--text-secondary:#525252;--border:#8d8d8d;--border-subtle:#c6c6c6;
        --link:#0f62fe;--highlight:#d0e2ff;--highlight-text:#001d6c;--focus:#0f62fe;
        --error:#da1e28;--error-bg:#fff1f1;--success:#198038;--success-bg:#defbe6;
        --warning:#8e6a00;--warning-icon:#f1c21b;--warning-bg:#fcf4d6;--info:#0043ce;--info-bg:#edf5ff;
        --disabled-bg:#c6c6c6;--disabled-text:#525252;
    """.trimIndent()
    private val dark = """
        color-scheme:dark;--background:#161616;--layer:#262626;--layer-hover:#333333;--field:#393939;
        --text:#f4f4f4;--text-secondary:#c6c6c6;--border:#8d8d8d;--border-subtle:#525252;
        --link:#78a9ff;--highlight:#0043ce;--highlight-text:#ffffff;--focus:#ffffff;
        --error:#ff8389;--error-bg:#520408;--success:#42be65;--success-bg:#022d0d;
        --warning:#f1c21b;--warning-icon:#f1c21b;--warning-bg:#302400;--info:#78a9ff;--info-bg:#001d6c;
        --disabled-bg:#525252;--disabled-text:#c6c6c6;
    """.trimIndent()
    val css = """
        :root{$light;--button-primary:#0f62fe;--button-hover:#0353e9;--button-active:#002d9c;--on-button:#ffffff}
        @media(prefers-color-scheme:dark){:root:not([data-theme=light]){$dark}}
        :root[data-theme=dark]{$dark}
        *{box-sizing:border-box}body{margin:0;background:var(--background);color:var(--text);font:16px/1.5 'IBM Plex Sans',system-ui,sans-serif}
        a{color:var(--link)}a:hover{text-decoration-thickness:2px}::selection{background:var(--highlight);color:var(--highlight-text)}
        input,select,textarea{font:inherit;color:var(--text);background:var(--field);border:1px solid var(--border);border-radius:4px;padding:10px}
        button{font:inherit;min-height:44px;padding:10px 20px;background:var(--button-primary);color:var(--on-button);border:0;border-radius:4px;cursor:pointer}
        button:hover{background:var(--button-hover)}button:active{background:var(--button-active)}
        button:disabled{background:var(--disabled-bg);color:var(--disabled-text);cursor:not-allowed}
        :focus-visible{outline:2px solid var(--focus);outline-offset:3px}input:focus,textarea:focus,select:focus{outline:2px solid var(--focus);outline-offset:-2px}
        [aria-invalid=true]{outline:2px solid var(--error)}button.danger{background:#da1e28;color:#ffffff}button.danger:hover{background:#ba1b23}
        .notice,[role=status]:not(:empty){color:var(--info);background:var(--info-bg);border-inline-start:3px solid var(--info);padding:12px}
        .error,[role=alert]{color:var(--error);background:var(--error-bg);border-inline-start:3px solid var(--error);padding:12px}
        .success{color:var(--success);background:var(--success-bg);border-inline-start:3px solid var(--success);padding:12px}
        .warning{color:var(--warning);background:var(--warning-bg);border-inline-start:3px solid var(--warning-icon);padding:12px}
        .theme-switcher{display:flex;justify-content:flex-end;align-items:center;gap:8px;padding:12px 0;color:var(--text-secondary)}
        .theme-switcher label{margin:0}.theme-switcher select{width:auto;min-width:100px;margin:0}.status{padding:4px 8px;font-size:.875rem;font-weight:600;background:var(--layer-hover)}
        .status-approved,.status-issued{color:var(--success);background:var(--success-bg)}.status-returned,.status-void{color:var(--error);background:var(--error-bg)}
        .status-checked{color:var(--warning);background:var(--warning-bg)}.status-submitted{color:var(--info);background:var(--info-bg)}
        @media print{.theme-switcher{display:none}:root,:root[data-theme]{$light}body{background:#fff;color:#161616}}
    """.trimIndent()
    val js = """
        (()=>{'use strict';const media=window.matchMedia('(prefers-color-scheme: dark)');let mode='system';
        try{const saved=localStorage.getItem('archiman-theme');if(['system','light','dark'].includes(saved))mode=saved;}catch(e){}
        const apply=()=>{document.documentElement.dataset.theme=mode==='system'?(media.matches?'dark':'light'):mode;};apply();
        const change=()=>{if(mode==='system')apply();};if(media.addEventListener)media.addEventListener('change',change);else media.addListener(change);
        const mount=()=>{const main=document.querySelector('main');if(!main)return;for(const form of document.querySelectorAll('form')){const action=form.querySelector('input[name=action]')?.value||'';if(/Delete$|Remove$|Void$/.test(action)||action==='archiveItem'){for(const button of form.querySelectorAll('button'))button.classList.add('danger');}}const bar=document.createElement('div');bar.className='theme-switcher';
        const label=document.createElement('label');label.textContent='Theme';label.htmlFor='appearance-mode';
        const select=document.createElement('select');select.id='appearance-mode';select.setAttribute('aria-label','Color theme');
        for(const value of ['system','light','dark']){const option=document.createElement('option');option.value=value;option.textContent=value[0].toUpperCase()+value.slice(1);select.append(option);}
        select.value=mode;select.addEventListener('change',()=>{mode=select.value;try{localStorage.setItem('archiman-theme',mode);}catch(e){}apply();});
        bar.append(label,select);main.prepend(bar);};if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount);else mount();})();
    """.trimIndent()
}
