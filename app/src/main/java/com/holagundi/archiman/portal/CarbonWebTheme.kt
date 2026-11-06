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
        .cds--content{line-height:1.43}
        .portal-canvas{max-width:99rem;padding-block:1.5rem 3rem}
        .cds--content h1{font-size:2rem;font-weight:400;line-height:1.25;margin:0 0 .5rem}
        .cds--content h2{font-size:1.25rem;font-weight:400;line-height:1.4;margin:0 0 .5rem;display:flex;align-items:center;gap:.625rem}
        .cds--content p,.cds--content li{font-size:.875rem;line-height:1.43;letter-spacing:.16px}
        .portal-lead{color:var(--cds-text-secondary,#525252);max-width:42rem;margin:0 0 1.5rem}
        .portal-kicker{margin:0 0 .5rem;color:var(--link);font-size:.75rem;font-weight:600;letter-spacing:.32px;text-transform:uppercase}
        .portal-stack{display:flex;flex-direction:column;gap:1rem}
        .portal-tiles{display:grid;grid-template-columns:repeat(auto-fill,minmax(18rem,1fr));gap:1rem;margin:0 0 2rem}
        a.cds--tile--clickable,.portal-tile{display:flex;flex-direction:row;align-items:flex-start;gap:1rem;min-block-size:7.5rem;padding:1.25rem;color:inherit;text-decoration:none;transition:background-color .12s ease}
        a.cds--tile--clickable:hover,.portal-tile:hover{background:var(--layer-hover)}
        .portal-tile-icon{display:inline-flex;align-items:center;justify-content:center;inline-size:2.75rem;block-size:2.75rem;flex:0 0 auto;background:var(--highlight);color:var(--highlight-text);border-radius:0}
        .portal-tile-glyph{width:1.25rem;height:1.25rem;fill:currentColor}
        .portal-tile-copy{display:flex;flex-direction:column;gap:.35rem;flex:1;min-inline-size:0}
        .portal-tile-copy h2{margin:0;font-size:1rem;font-weight:600}
        .portal-tile-meta{color:var(--cds-text-secondary,#525252);margin:0}
        .portal-tile-arrow{margin-inline-start:auto;align-self:center;opacity:.65;fill:currentColor}
        .portal-panel{padding:1.25rem}
        .portal-panel h2 .carbon-icon{fill:var(--link)}
        .portal-masthead{display:flex;align-items:flex-start;gap:1rem;margin:0 0 1.75rem;padding:1.25rem 1.25rem 1.25rem 0;border-block-end:1px solid var(--border-subtle)}
        .portal-masthead-icon{display:inline-flex;align-items:center;justify-content:center;inline-size:3rem;block-size:3rem;background:var(--highlight);color:var(--highlight-text);flex:0 0 auto}
        .portal-masthead-glyph{width:1.5rem;height:1.5rem;fill:currentColor}
        .portal-masthead .portal-lead{margin-block-end:0}
        .portal-section-title{display:flex;align-items:center;gap:.625rem;margin:1.5rem 0 1rem}
        .portal-section-title .carbon-icon{fill:var(--link)}
        .carbon-icon{display:inline-block;width:1.25rem;height:1.25rem;fill:currentColor;flex-shrink:0;vertical-align:middle}
        .carbon-inline-icon{width:1rem;height:1rem;margin-inline-end:.35rem}
        .carbon-kpi-icon{width:1.25rem;height:1.25rem;margin-block-end:.5rem;fill:var(--link)}
        .cds--btn{display:inline-flex;align-items:center;gap:.75rem}
        .cds--btn .carbon-icon{width:1rem;height:1rem}
        .cds--header__name{display:inline-flex;align-items:center;gap:.5rem}
        .portal-brand-icon{width:1.25rem;height:1.25rem;fill:currentColor}
        .cds--header__menu-item{display:inline-flex;align-items:center;gap:.5rem}
        .cds--content ul{margin:.5rem 0 0 1rem;list-style:disc}
        .cds--content .cds--form-item{margin-block-end:1.5rem}
        .cds--content .cds--label{display:block;margin-block-end:.5rem}
        .cds--content .cds--text-input,.cds--content textarea.cds--text-input,.cds--content .cds--select-input{inline-size:100%}
        .cds--content form{border:0!important;background:transparent!important;padding:0!important;margin:0}
        .portal-body--auth .cds--content{display:grid;place-items:center;min-block-size:calc(100vh - 3rem)}
        .portal-sign-in{max-inline-size:28rem;margin-inline:auto;padding:2rem;box-shadow:0 2px 6px rgba(0,0,0,.08)}
        .portal-sign-in-brand{display:inline-flex;align-items:center;justify-content:center;inline-size:3.5rem;block-size:3.5rem;margin-block-end:1rem;background:var(--highlight);color:var(--highlight-text)}
        .portal-sign-in-glyph{width:1.75rem;height:1.75rem;fill:currentColor}
        .portal-file{display:block;margin-block-start:.5rem;font-size:.875rem}
        .cds--side-nav.cds--side-nav--expanded{inset-block-start:3rem;inline-size:16rem}
        .cds--side-nav__navigation a.cds--side-nav__link{display:flex;align-items:center;gap:.75rem;border:0;padding-inline:1rem;min-block-size:2.5rem;text-decoration:none}
        .cds--side-nav__icon{width:1rem;height:1rem;fill:currentColor}
        .cds--side-nav__link--current{background:var(--highlight);color:var(--highlight-text);font-weight:600}
        .cds--header__global{display:flex;align-items:center;gap:.5rem;margin-inline-start:auto;padding-inline-end:1rem;block-size:100%}
        .cds--header__global .theme-switcher{padding:0}
        .cds--header__global .theme-switcher select{min-block-size:2rem;inline-size:auto}
        .portal-sign-out{margin:0}
        .portal-sign-out .cds--btn{min-block-size:2.5rem;padding-inline:.75rem}
        .cds--header .cds--header__nav{display:none!important}
        @media(max-width:42rem){
          .cds--side-nav{display:none}
          .cds--side-nav.cds--side-nav--expanded~.cds--content{margin-inline-start:0}
          .cds--header .cds--header__nav{display:block!important}
          .portal-masthead{flex-direction:column;gap:.75rem}
          a.cds--tile--clickable,.portal-tile{min-block-size:0}
        }
        cds-breadcrumb{display:block;margin:0 0 1rem}
    """.trimIndent()
    val js = """
        (()=>{'use strict';const media=window.matchMedia('(prefers-color-scheme: dark)');let mode='system';
        try{const saved=localStorage.getItem('archiman-theme');if(['system','light','dark'].includes(saved))mode=saved;}catch(e){}
        const apply=()=>{document.documentElement.dataset.theme=mode==='system'?(media.matches?'dark':'light'):mode;};apply();
        const change=()=>{if(mode==='system')apply();};if(media.addEventListener)media.addEventListener('change',change);else media.addListener(change);
        const mount=()=>{const main=document.querySelector('main');if(!main)return;for(const form of document.querySelectorAll('form')){const action=form.querySelector('input[name=action]')?.value||'';if(/Delete$|Remove$|Void$/.test(action)||action==='archiveItem'){for(const button of form.querySelectorAll('button'))button.classList.add('danger');}}if(document.getElementById('appearance-mode'))return;const bar=document.createElement('div');bar.className='theme-switcher';
        const label=document.createElement('label');label.textContent='Theme';label.htmlFor='appearance-mode';
        const select=document.createElement('select');select.id='appearance-mode';select.setAttribute('aria-label','Color theme');
        for(const value of ['system','light','dark']){const option=document.createElement('option');option.value=value;option.textContent=value[0].toUpperCase()+value.slice(1);select.append(option);}
        select.value=mode;select.addEventListener('change',()=>{mode=select.value;try{localStorage.setItem('archiman-theme',mode);}catch(e){}apply();});
        bar.append(label,select);const host=document.querySelector('.cds--header__global');if(host)host.prepend(bar);else main.prepend(bar);};if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount);else mount();})();
    """.trimIndent()
}
