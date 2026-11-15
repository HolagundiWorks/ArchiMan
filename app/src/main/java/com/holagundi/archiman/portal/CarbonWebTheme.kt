package com.holagundi.archiman.portal

/** Shared by the sign-in/project pages and the administrative workspace. */
object CarbonWebTheme {
    private val light = """
        color-scheme:light;--background:#f4f4f4;--layer:#ffffff;--layer-01:#ffffff;--layer-hover:#e8e8e8;--field:#ffffff;
        --text:#161616;--text-secondary:#525252;--border:#8d8d8d;--border-subtle:#c6c6c6;
        --link:#b83e16;--interactive:#b83e16;--highlight:#f8e8e0;--highlight-text:#6e2008;--focus:#b83e16;
        --error:#da1e28;--error-bg:#fff1f1;--success:#198038;--success-bg:#defbe6;
        --warning:#8e6a00;--warning-icon:#f1c21b;--warning-bg:#fcf4d6;--info:#0043ce;--info-bg:#edf5ff;
        --disabled-bg:#c6c6c6;--disabled-text:#525252;--brand:#161616;--brand-text:#f4f4f4;--rail:#e0e0e0;
        --shadow:0 2px 6px rgba(0,0,0,.2);
    """.trimIndent()
    private val dark = """
        color-scheme:dark;--background:#161616;--layer:#262626;--layer-01:#262626;--layer-hover:#333333;--field:#393939;
        --text:#f4f4f4;--text-secondary:#c6c6c6;--border:#8d8d8d;--border-subtle:#525252;
        --link:#e0703a;--interactive:#e0703a;--highlight:#3a2218;--highlight-text:#ffffff;--focus:#ffffff;
        --error:#ff8389;--error-bg:#520408;--success:#42be65;--success-bg:#022d0d;
        --warning:#f1c21b;--warning-icon:#f1c21b;--warning-bg:#302400;--info:#78a9ff;--info-bg:#001d6c;
        --disabled-bg:#525252;--disabled-text:#c6c6c6;--brand:#161616;--brand-text:#f4f4f4;--rail:#262626;
        --shadow:0 2px 6px rgba(0,0,0,.6);
    """.trimIndent()
    val css = """
        :root{$light;--button-primary:#b83e16;--button-hover:#a03212;--button-active:#8c2a10;--on-button:#ffffff;--header-height:3rem;--side-rail:3rem;--side-expanded:16rem}
        @media(prefers-color-scheme:dark){:root:not([data-theme=light]){$dark}}
        :root[data-theme=dark]{$dark}
        *{box-sizing:border-box}html,body{max-inline-size:100%;overflow-x:hidden}
        body{margin:0;background:var(--background);color:var(--text);font:16px/1.5 'IBM Plex Sans',system-ui,sans-serif}
        a{color:var(--link)}a:hover{text-decoration-thickness:2px}::selection{background:var(--highlight);color:var(--highlight-text)}
        input,select,textarea{font:inherit;color:var(--text);background:var(--field);border:0;border-block-end:1px solid var(--border);border-radius:0;padding:10px;inline-size:100%;max-inline-size:100%}
        button{font:inherit;min-height:2.5rem;padding:10px 16px;background:var(--button-primary);color:var(--on-button);border:0;border-radius:0;cursor:pointer}
        button:hover{background:var(--button-hover)}button:active{background:var(--button-active)}
        button:disabled{background:var(--disabled-bg);color:var(--disabled-text);cursor:not-allowed}
        :focus-visible{outline:2px solid var(--focus);outline-offset:2px}input:focus,textarea:focus,select:focus{outline:2px solid var(--focus);outline-offset:-2px}
        [aria-invalid=true]{outline:2px solid var(--error)}button.danger{background:#da1e28;color:#ffffff}button.danger:hover{background:#ba1b23}
        .notice,[role=status]:not(:empty){color:var(--info);background:var(--info-bg);border-inline-start:3px solid var(--info);padding:12px;overflow-wrap:anywhere}
        .error,[role=alert]{color:var(--error);background:var(--error-bg);border-inline-start:3px solid var(--error);padding:12px;overflow-wrap:anywhere}
        .success{color:var(--success);background:var(--success-bg);border-inline-start:3px solid var(--success);padding:12px}
        .warning{color:var(--warning);background:var(--warning-bg);border-inline-start:3px solid var(--warning-icon);padding:12px;overflow-wrap:anywhere}
        .status{display:inline-block;padding:2px 8px;font-size:.75rem;font-weight:600;background:var(--layer-hover);max-inline-size:100%;overflow-wrap:anywhere}
        .status-approved,.status-issued{color:var(--success);background:var(--success-bg)}.status-returned,.status-void{color:var(--error);background:var(--error-bg)}
        .status-checked{color:var(--warning);background:var(--warning-bg)}.status-submitted{color:var(--info);background:var(--info-bg)}
        @media print{.portal-theme-toggle,#portal-search,#portal-nav-toggle,.portal-side-nav{display:none!important}:root,:root[data-theme]{$light}body{background:#fff;color:#161616}}
        .cds--header{position:fixed;inset-block-start:0;inset-inline:0;z-index:8000;display:flex;align-items:center;block-size:var(--header-height);background:var(--brand);color:var(--brand-text);border-block-end:1px solid #393939}
        .cds--header__name{display:inline-flex;align-items:center;gap:.5rem;color:var(--brand-text)!important;text-decoration:none;padding-inline:1rem;font-weight:600;min-inline-size:0}
        .cds--header__name--prefix{font-weight:600}.portal-brand-product{font-weight:400;color:#c6c6c6;margin-inline-start:.25rem}
        .portal-brand-icon{width:1.25rem;height:1.25rem;fill:currentColor;flex-shrink:0}
        .portal-brand-logo{width:1.75rem;height:1.75rem;object-fit:contain;flex-shrink:0;background:#fff;border-radius:2px}
        .portal-brand-product{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-inline-size:14rem}
        .portal-side-brand{display:flex;align-items:center;gap:.75rem;padding:1rem;border-block-end:1px solid var(--border-subtle);min-block-size:4rem;overflow:hidden}
        .portal-side-brand .portal-brand-logo,.portal-side-brand .portal-brand-icon{width:2rem;height:2rem}
        .portal-side-brand .portal-brand-icon{fill:var(--text)}
        .portal-side-brand__copy{min-inline-size:0;display:flex;flex-direction:column;gap:.15rem}
        .portal-side-brand__copy strong{font-size:.8125rem;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
        .portal-side-brand__copy span{font-size:.6875rem;color:var(--text-secondary)}
        .portal-side-nav[data-state=rail] .portal-side-brand__copy{display:none}
        .portal-side-nav[data-state=rail] .portal-side-brand{justify-content:center;padding:.75rem .5rem}
        .portal-sign-in-logo{display:block;width:4.5rem;height:4.5rem;object-fit:contain;margin-block-end:1rem;background:#fff;border:1px solid var(--border-subtle);padding:.35rem}
        .cds--header__action,.cds--header__menu-toggle{display:inline-flex;align-items:center;justify-content:center;inline-size:3rem;block-size:3rem;min-height:3rem;padding:0;background:transparent;color:var(--brand-text);border:0}
        .cds--header__action:hover,.cds--header__menu-toggle:hover{background:#353535}
        .cds--header__action .carbon-icon,.cds--header__menu-toggle .carbon-icon{width:1.25rem;height:1.25rem;fill:currentColor}
        .cds--header__global{display:flex;align-items:center;margin-inline-start:auto;block-size:100%}
        .portal-sign-out{margin:0}.portal-sign-out .cds--btn{background:transparent;color:var(--brand-text);min-height:3rem;border-radius:0}
        .portal-sign-out .cds--btn:hover{background:#353535}.portal-action-label{margin-inline-start:.5rem}
        .cds--header .cds--header__nav{display:none!important}
        .portal-side-nav.cds--side-nav{position:fixed;inset-block-start:var(--header-height);inset-inline-start:0;inset-block-end:0;z-index:8500;inline-size:var(--side-rail);background:var(--layer);border-inline-end:1px solid var(--border-subtle);overflow-x:hidden;overflow-y:auto;transition:inline-size .16s ease;pointer-events:auto;transform:none!important;visibility:visible!important}
        .portal-side-nav[data-state=expanded],.portal-side-nav:hover,.portal-side-nav:focus-within,.portal-side-nav.portal-side-nav--hold{inline-size:var(--side-expanded)}
        .portal-body--app .cds--content{margin-inline-start:var(--side-rail);margin-block-start:var(--header-height);padding:0 1.5rem;min-block-size:calc(100vh - var(--header-height));transition:margin-inline-start .16s ease;position:relative;z-index:1}
        .portal-body--app.portal-nav-open .cds--content,.portal-body--app:has(.portal-side-nav[data-state=expanded]) .cds--content{margin-inline-start:var(--side-expanded)}
        .portal-nav-items{list-style:none;margin:0;padding:.5rem 0;overflow:visible!important;flex:none!important}
        .portal-nav-group{margin:0;overflow:visible!important}
        .portal-nav-title{display:none;padding:.75rem 1rem .25rem;font-size:.75rem;font-weight:600;letter-spacing:.32px;text-transform:uppercase;color:var(--text-secondary)}
        .portal-side-nav[data-state=expanded] .portal-nav-title,.portal-side-nav:hover .portal-nav-title,.portal-side-nav:focus-within .portal-nav-title,.portal-side-nav.portal-side-nav--hold .portal-nav-title{display:block}
        .portal-nav-menu{list-style:none;margin:0;padding:0;display:block!important;max-block-size:none!important;visibility:visible!important;overflow:visible!important}
        .portal-side-nav a.cds--side-nav__link{display:flex!important;align-items:center;gap:.75rem;border:0;padding:0!important;padding-inline:0!important;min-block-size:3rem!important;block-size:3rem!important;inline-size:100%;text-decoration:none;color:var(--text);justify-content:center;pointer-events:auto;cursor:pointer;position:relative;z-index:1}
        .portal-side-nav[data-state=expanded] a.cds--side-nav__link,.portal-side-nav:hover a.cds--side-nav__link,.portal-side-nav:focus-within a.cds--side-nav__link,.portal-side-nav.portal-side-nav--hold a.cds--side-nav__link{justify-content:flex-start;padding-inline:1rem!important}
        .portal-side-nav .cds--side-nav__link-text{display:none;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;pointer-events:none}
        .portal-side-nav[data-state=expanded] .cds--side-nav__link-text,.portal-side-nav:hover .cds--side-nav__link-text,.portal-side-nav:focus-within .cds--side-nav__link-text,.portal-side-nav.portal-side-nav--hold .cds--side-nav__link-text{display:inline}
        .portal-side-nav .cds--side-nav__icon{width:1.25rem;height:1.25rem;fill:currentColor;flex-shrink:0;pointer-events:none}
        .portal-side-nav .cds--side-nav__link--current{background:var(--highlight);color:var(--highlight-text);font-weight:600}
        .cds--content{line-height:1.43}
        .portal-canvas{max-width:99rem;padding-block:1.5rem 3rem;min-inline-size:0}
        .cds--content h1{font-size:2rem;font-weight:400;line-height:1.25;margin:0 0 .5rem;overflow-wrap:anywhere}
        .cds--content h2{font-size:1.25rem;font-weight:400;line-height:1.4;margin:0 0 .5rem;display:flex;align-items:center;gap:.625rem;min-inline-size:0;overflow-wrap:anywhere}
        .cds--content p,.cds--content li{font-size:.875rem;line-height:1.43;letter-spacing:.16px;overflow-wrap:anywhere}
        .portal-lead{color:var(--text-secondary);max-width:42rem;margin:0 0 1.5rem}
        .portal-kicker{margin:0 0 .5rem;color:var(--link);font-size:.75rem;font-weight:600;letter-spacing:.32px;text-transform:uppercase}
        .portal-stack{display:flex;flex-direction:column;gap:1rem;min-inline-size:0}
        .portal-tiles{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:.5rem;margin:0 0 2rem}
        a.cds--tile--clickable,.portal-tile,.cds--tile{display:flex;flex-direction:row;align-items:flex-start;gap:1rem;min-block-size:9.5rem;padding:1.25rem;color:inherit;text-decoration:none;background:var(--layer);border:1px solid var(--border-subtle);border-block-start:3px solid var(--border-subtle);overflow:hidden;min-inline-size:0}
        a.cds--tile--clickable:hover,.portal-tile:hover{background:var(--layer-hover)}
        .portal-tile--ok,.cds--tile.portal-tile--ok{border-block-start-color:#24a148}
        .portal-tile--warning,.cds--tile.portal-tile--warning{border-block-start-color:#f1c21b}
        .portal-tile--critical,.cds--tile.portal-tile--critical{border-block-start-color:var(--error,#da1e28)}
        .portal-tile-icon{display:inline-flex;align-items:center;justify-content:center;inline-size:2.75rem;block-size:2.75rem;flex:0 0 auto;background:var(--highlight);color:var(--highlight-text)}
        .portal-tile-glyph{width:1.25rem;height:1.25rem;fill:currentColor}
        .portal-tile-copy{display:flex;flex-direction:column;gap:.35rem;flex:1;min-inline-size:0;overflow:hidden}
        .portal-tile-copy h2{margin:0;font-size:1rem;font-weight:600;overflow:hidden;text-overflow:ellipsis;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
        .portal-tile-meta{color:var(--text-secondary);margin:0;overflow:hidden;text-overflow:ellipsis;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
        .portal-tile-arrow{margin-inline-start:auto;align-self:center;opacity:.65;fill:currentColor;flex-shrink:0}
        .portal-panel{padding:1.25rem;overflow:hidden;min-inline-size:0}
        .portal-panel h2 .carbon-icon{fill:var(--link);flex-shrink:0}
        .portal-masthead{display:flex;align-items:flex-start;gap:1rem;margin:0 0 1.75rem;padding:0 0 1.25rem;border-block-end:1px solid var(--border-subtle);min-inline-size:0}
        .portal-masthead-icon{display:inline-flex;align-items:center;justify-content:center;inline-size:3rem;block-size:3rem;background:var(--highlight);color:var(--highlight-text);flex:0 0 auto}
        .portal-masthead-glyph{width:1.5rem;height:1.5rem;fill:currentColor}
        .portal-masthead-copy{min-inline-size:0;flex:1}.portal-masthead .portal-lead{margin-block-end:0}
        .portal-section-title{display:flex;align-items:center;gap:.625rem;margin:1.5rem 0 1rem}
        .portal-section-title .carbon-icon{fill:var(--link)}
        .carbon-icon{display:inline-block;width:1.25rem;height:1.25rem;fill:currentColor;flex-shrink:0;vertical-align:middle}
        .carbon-inline-icon{width:1rem;height:1rem;margin-inline-end:.35rem}
        .carbon-kpi-icon{width:1.25rem;height:1.25rem;margin-block-end:.5rem;fill:var(--link)}
        .cds--btn{display:inline-flex;align-items:center;gap:.75rem;max-inline-size:100%}
        .cds--btn .carbon-icon{width:1rem;height:1rem}
        .cds--content ul{margin:.5rem 0 0 1rem;list-style:disc;padding:0;overflow-wrap:anywhere}
        .cds--content .cds--form-item{margin-block-end:1rem;min-inline-size:0}
        .cds--content .cds--label{display:block;margin-block-end:.5rem;color:var(--text-secondary);font-size:.75rem;font-weight:600;letter-spacing:.32px}
        .cds--content .cds--text-input,.cds--content textarea.cds--text-input,.cds--content .cds--select-input{inline-size:100%;max-inline-size:100%}
        .cds--content form{border:0!important;background:transparent!important;padding:0!important;margin:0 0 1.5rem;max-inline-size:100%;overflow:hidden}
        .portal-form-section{border:1px solid var(--border-subtle);background:var(--layer);padding:1.25rem;margin:0 0 1rem;min-inline-size:0;overflow:hidden}
        .portal-form-legend{padding:0;font-size:.875rem;font-weight:600;color:var(--text);margin:0 0 .75rem}
        .portal-form-hint{margin:0 0 1rem;color:var(--text-secondary);font-size:.875rem}
        .portal-form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem 1.5rem}
        .portal-form-grid .cds--form-item:has(textarea),.portal-form-span-2{grid-column:1 / -1}
        .portal-body--auth .cds--content{display:grid;place-items:center;min-block-size:100vh;margin:0;padding:1.5rem}
        .portal-sign-in{max-inline-size:28rem;inline-size:100%;margin-inline:auto;padding:2rem;background:var(--layer);border:1px solid var(--border-subtle);box-shadow:var(--shadow);overflow:hidden}
        .portal-sign-in-brand{display:inline-flex;align-items:center;justify-content:center;inline-size:3.5rem;block-size:3.5rem;margin-block-end:1rem;background:var(--highlight);color:var(--highlight-text)}
        .portal-sign-in-glyph{width:1.75rem;height:1.75rem;fill:currentColor}
        .portal-file{display:block;margin-block-start:.5rem;font-size:.875rem;max-inline-size:100%}
        .portal-search{position:fixed;inset:0;z-index:9000;display:grid;place-items:start center;padding:4.5rem 1rem 1rem;background:rgba(22,22,22,.45)}
        .portal-search[hidden]{display:none!important}
        .portal-search__panel{inline-size:min(40rem,100%);background:var(--layer);border:1px solid var(--border-subtle);box-shadow:var(--shadow);overflow:hidden}
        .portal-search__bar{display:flex;align-items:center;gap:.75rem;padding:.75rem 1rem;border-block-end:1px solid var(--border-subtle)}
        .portal-search__glyph{fill:var(--text-secondary)}
        .portal-search__input{border:0!important;outline:0!important;background:transparent;padding:.25rem 0;min-height:2rem}
        .portal-search__close{min-height:2.5rem;inline-size:2.5rem;padding:0;background:transparent;color:var(--text)}
        .portal-search__results{list-style:none;margin:0;padding:.5rem 0;max-block-size:min(24rem,50vh);overflow:auto}
        .portal-search__results a{display:flex;flex-direction:column;gap:.15rem;padding:.75rem 1rem;text-decoration:none;color:var(--text)}
        .portal-search__results a:hover,.portal-search__results a:focus{background:var(--layer-hover)}
        .portal-search__results small{color:var(--text-secondary)}
        .portal-search__hint{margin:0;padding:.75rem 1rem;color:var(--text-secondary);font-size:.75rem;border-block-start:1px solid var(--border-subtle)}
        .portal-search__hint kbd{font:inherit;padding:0 .25rem;border:1px solid var(--border-subtle);background:var(--layer-hover)}
        .carbon-kpis,.carbon-insights,.carbon-insight,.carbon-project-context,article,.paper{max-inline-size:100%;overflow:hidden;min-inline-size:0}
        .tablewrap,.portal-sheet-wrap{max-inline-size:100%;min-inline-size:0}
        .carbon-tile-grid{display:grid;gap:.5rem;margin:0 0 1rem;min-inline-size:0}
        .carbon-tile-grid--small{grid-template-columns:repeat(5,minmax(0,1fr))}
        .carbon-tile-grid--medium{grid-template-columns:repeat(3,minmax(0,1fr))}
        .carbon-tile-grid--large{grid-template-columns:1fr}
        .carbon-tile--small{min-block-size:6rem;block-size:6rem}
        .carbon-tile--medium,.carbon-tile--large{min-block-size:9.5rem}
        details.carbon-tile--expandable{background:var(--layer);border:1px solid var(--border-subtle);border-block-start:3px solid var(--border-subtle);min-block-size:9.5rem;padding:0}
        details.carbon-tile--expandable>summary{list-style:none;cursor:pointer;display:flex;align-items:center;justify-content:space-between;gap:1rem;min-block-size:calc(9.5rem - 3px);padding:1rem 1.25rem;font-weight:600}
        details.carbon-tile--expandable>summary::-webkit-details-marker{display:none}
        details.carbon-tile--expandable:not([open]){block-size:9.5rem;overflow:hidden}
        details.carbon-tile--expandable[open]{block-size:auto;min-block-size:9.5rem}
        details.carbon-tile--expandable .carbon-tile-expand__body{padding:0 1.25rem 1.25rem}
        details.carbon-tile--expandable.portal-tile--ok,details.carbon-tile--expandable.carbon-kpi--ok{border-block-start-color:#24a148}
        details.carbon-tile--expandable.portal-tile--warning,details.carbon-tile--expandable.carbon-kpi--warning{border-block-start-color:#f1c21b}
        details.carbon-tile--expandable.portal-tile--critical,details.carbon-tile--expandable.carbon-kpi--critical{border-block-start-color:var(--error,#da1e28)}
        .carbon-kpis{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:.5rem;margin:1.5rem 0;background:transparent;border:0}
        .carbon-kpi{background:var(--layer);padding:1rem;display:flex;flex-direction:column;min-inline-size:0;border:1px solid var(--border-subtle);border-block-start:3px solid var(--border-subtle);min-block-size:6rem;block-size:6rem}
        .carbon-kpi--ok{border-block-start-color:#24a148}
        .carbon-kpi--warning{border-block-start-color:#f1c21b}
        .carbon-kpi--critical{border-block-start-color:var(--error,#da1e28)}
        .carbon-kpi-icon{margin-block-end:.35rem;fill:var(--link)}
        .carbon-kpi strong{font-size:1.25rem;font-weight:400;display:block;line-height:1.2}
        .carbon-kpi span{font-size:.75rem;color:var(--text-secondary)}
        .portal-filing-board{margin:1.5rem 0 2rem}
        .portal-filing-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1px;background:var(--border-subtle);border:1px solid var(--border-subtle)}
        .portal-filing{background:var(--layer);padding:1.25rem;min-inline-size:0;border-block-start:3px solid var(--link)}
        .portal-filing h3{margin:0 0 .35rem;font-size:.875rem;font-weight:600}
        .portal-filing-date{margin:0;font-size:1.25rem;font-weight:400}
        .portal-filing-meta{margin:.35rem 0 0;font-size:.75rem;color:var(--text-secondary)}
        .portal-filing--soon{border-block-start-color:#f1c21b}
        .portal-filing--overdue{border-block-start-color:var(--error,#da1e28)}
        .portal-filing--ok{border-block-start-color:#24a148}
        .portal-actions{display:flex;flex-wrap:wrap;gap:.75rem;margin:0 0 1.5rem}
        .portal-segment{display:inline-flex;margin:0 0 1.25rem;border:1px solid var(--border-subtle);background:var(--layer);padding:0}
        .portal-segment__item{display:inline-flex;align-items:center;gap:.5rem;padding:.75rem 1.25rem;min-height:2.5rem;text-decoration:none;color:var(--text);border-inline-end:1px solid var(--border-subtle)}
        .portal-segment__item:last-child{border-inline-end:0}
        .portal-segment__item:hover,.portal-segment__item:focus{background:var(--layer-hover)}
        .portal-segment__item.is-active{background:var(--highlight);color:var(--highlight-text)}
        .portal-segment__item.is-active svg{fill:currentColor}
        .portal-segment__item svg{fill:var(--link)}
        .portal-contact-form .portal-actions{margin-block-start:1rem}
        .portal-kanban{margin:1.5rem 0}
        .portal-kanban-tabs{display:flex;gap:2px;align-items:stretch;margin:0;overflow-x:auto}
        .portal-kanban-tab{appearance:none;display:flex;flex-direction:column;align-items:flex-start;gap:2px;flex:1;min-inline-size:6rem;min-block-size:3rem;background:var(--layer-hover);border:0;border-block-start:3px solid var(--layer-hover);color:var(--text-secondary);cursor:pointer;font:inherit;font-size:.875rem;font-weight:500;padding:.5rem 1rem;text-align:start}
        .portal-kanban-tab:hover{background:color-mix(in srgb,var(--layer-hover) 85%,var(--text) 15%)}
        .portal-kanban-tab[aria-selected="true"]{background:var(--layer);border-block-start-color:var(--interactive);color:var(--text);font-weight:600}
        .portal-kanban-tab__label{display:block;line-height:1.25}
        .portal-kanban-tab__count{font-size:.75rem;font-weight:400;color:var(--text-secondary);line-height:1.25}
        .portal-kanban-tab[aria-selected="true"] .portal-kanban-tab__count{color:var(--text-secondary)}
        .portal-kanban-column{background:var(--layer);display:none;flex-direction:column;min-inline-size:0;min-block-size:18rem;padding:1rem 1rem 1.25rem;border:1px solid var(--border-subtle);border-block-start:0}
        .portal-kanban-column.is-active{display:flex}
        .portal-kanban-column.is-drop{outline:2px solid var(--interactive);outline-offset:-2px}
        .portal-kanban-column__head{display:flex;align-items:center;justify-content:space-between;gap:.75rem;padding:0 0 1rem;margin:0 0 .25rem;border-block-end:1px solid var(--border-subtle)}
        .portal-kanban-column__head h2{margin:0;font-size:.875rem;font-weight:600}
        .portal-kanban-column__count{font-size:.75rem;color:var(--text-secondary)}
        .portal-kanban-column__body{flex:1;padding:0;display:flex;flex-direction:column;gap:.75rem;overflow:auto}
        .portal-kanban-card{background:var(--layer);border:1px solid var(--border-subtle);padding:0;cursor:grab;box-shadow:none;border-block-start:3px solid var(--border-subtle)}
        .portal-kanban-card--ok{border-block-start-color:#24a148}
        .portal-kanban-card--warning{border-block-start-color:#f1c21b}
        .portal-kanban-card--critical{border-block-start-color:var(--error,#da1e28)}
        .portal-kanban-card:hover{background:var(--layer-hover)}
        .portal-kanban-card:active{cursor:grabbing}
        .portal-kanban-card .cds--tag{display:inline-block;margin:.75rem 1rem 0;padding:.125rem .5rem;font-size:.75rem;font-weight:500;line-height:1.25;background:var(--layer-hover);color:var(--text)}
        .portal-kanban-card .cds--tag--ok{background:color-mix(in srgb,#24a148 18%,var(--layer));color:#198038}
        .portal-kanban-card .cds--tag--warning{background:color-mix(in srgb,#f1c21b 28%,var(--layer));color:#8e6a00}
        .portal-kanban-card .cds--tag--critical{background:var(--error-bg,color-mix(in srgb,var(--error,#da1e28) 14%,var(--layer)));color:var(--error,#da1e28)}
        .portal-kanban-card__title{margin:.5rem 1rem .75rem;font-size:.875rem;font-weight:600;overflow-wrap:anywhere}
        .portal-kanban-card__actions{display:flex;justify-content:flex-end;gap:.25rem;padding:0 .5rem .5rem}
        .portal-kanban-move{display:inline-flex;margin:0}
        .portal-kanban-move__btn{min-inline-size:2.5rem;inline-size:2.5rem;padding:0;background:transparent;color:var(--text)}
        .portal-kanban-move__btn.is-back svg{transform:scaleX(-1)}
        .portal-kanban-empty{margin:0;color:var(--text-secondary);font-size:.75rem}
        .portal-kanban-add{display:grid;grid-template-columns:1fr minmax(8rem,10rem) auto;gap:1rem;align-items:end}
        .portal-overview-empty{margin:0;padding:.75rem 0;color:var(--text-secondary);font-size:.875rem}
        .portal-mix{display:grid;grid-template-columns:1fr 1fr;gap:0;margin:.75rem 0 1rem;border:1px solid var(--border-subtle);background:var(--layer)}
        .portal-mix--flush{margin:0 0 1rem;border:0;border-block-end:1px solid var(--border-subtle);background:transparent}
        .portal-mix__cell{display:flex;flex-direction:column;gap:.2rem;padding:.875rem 1rem;min-inline-size:0;border-block-start:3px solid var(--border-subtle)}
        .portal-mix--flush .portal-mix__cell{padding:.5rem 1rem .875rem 0}
        .portal-mix__cell--ok{border-block-start-color:#24a148}
        .portal-mix__cell--warning{border-block-start-color:#f1c21b}
        .portal-mix__cell--critical{border-block-start-color:var(--error,#da1e28)}
        .portal-mix__cell+ .portal-mix__cell{border-inline-start:1px solid var(--border-subtle)}
        .portal-mix--flush .portal-mix__cell+ .portal-mix__cell{padding-inline-start:1rem}
        .portal-mix__cell strong{font-size:1.75rem;font-weight:400;line-height:1.2}
        .portal-mix__cell span{font-size:.75rem;color:var(--text-secondary)}
        .portal-product{margin:0 0 2rem}
        .portal-product-header{display:flex;align-items:flex-start;justify-content:space-between;gap:1.5rem;margin:0 0 .25rem;flex-wrap:wrap}
        .portal-product-header__copy{min-inline-size:0;flex:1}
        .portal-product-crumb{margin:0 0 .5rem;font-size:.875rem;color:var(--text-secondary)}
        .portal-product-crumb a{color:var(--link);text-decoration:none}
        .portal-product-crumb a:hover{text-decoration:underline}
        .portal-product-header h1{margin:0;font-size:2rem;font-weight:400;line-height:1.25}
        .portal-product-actions{position:relative}
        .portal-product-actions>summary{list-style:none;cursor:pointer;display:inline-flex;align-items:center;gap:.5rem}
        .portal-product-actions>summary::-webkit-details-marker{display:none}
        .portal-product-actions>summary .carbon-icon{width:1rem;height:1rem;fill:currentColor}
        .portal-product-actions[open]>summary .carbon-icon{transform:rotate(180deg)}
        .portal-product-actions__menu{position:absolute;inset-block-start:calc(100% + .25rem);inset-inline-end:0;z-index:20;min-inline-size:14rem;background:var(--layer);border:1px solid var(--border-subtle);box-shadow:var(--shadow);padding:.25rem 0}
        .portal-product-actions__menu a{display:flex;align-items:center;gap:.75rem;padding:.625rem 1rem;color:var(--text);text-decoration:none;font-size:.875rem}
        .portal-product-actions__menu a:hover{background:var(--layer-hover)}
        .portal-product-actions__menu .carbon-icon{fill:var(--link)}
        .portal-product-tabs{display:flex;gap:0;align-items:stretch;margin:1rem 0 0;border-block-end:1px solid var(--border-subtle);overflow-x:auto}
        .portal-product-tab{appearance:none;background:transparent;border:0;border-block-end:2px solid transparent;color:var(--text-secondary);cursor:pointer;font:inherit;font-size:.875rem;font-weight:400;padding:.75rem 1rem;margin-block-end:-1px;white-space:nowrap}
        .portal-product-tab:hover{color:var(--text)}
        .portal-product-tab[aria-selected="true"]{color:var(--text);font-weight:600;border-block-end-color:var(--interactive)}
        .portal-product-tab__count{color:var(--text-secondary);font-weight:400}
        .portal-product-panel-host{display:none;padding:1.25rem 0 0}
        .portal-product-panel-host.is-active{display:block}
        .portal-product-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:.5rem;min-inline-size:0}
        .portal-product-panel{background:var(--layer);border:1px solid var(--border-subtle);border-block-start:3px solid var(--border-subtle);padding:1rem 1.25rem 1.25rem;margin:0;min-inline-size:0;min-block-size:9.5rem}
        .portal-product-panel--ok{border-block-start-color:#24a148}
        .portal-product-panel--warning{border-block-start-color:#f1c21b}
        .portal-product-panel--critical{border-block-start-color:var(--error,#da1e28)}
        .portal-product-panel__head{display:flex;align-items:baseline;justify-content:space-between;gap:1rem;margin:0 0 1rem}
        .portal-product-panel__head h2{margin:0;font-size:1rem;font-weight:600}
        .portal-product-panel__meta{font-size:.75rem;color:var(--text-secondary)}
        .portal-product-kpis{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:.5rem;margin:0 0 1.25rem}
        .portal-product-kpi{display:flex;flex-direction:column;gap:.15rem;min-inline-size:0;padding:.75rem;background:var(--layer);border:1px solid var(--border-subtle);border-block-start:3px solid var(--border-subtle);min-block-size:6rem;block-size:6rem}
        .portal-product-kpi--ok{border-block-start-color:#24a148}
        .portal-product-kpi--warning{border-block-start-color:#f1c21b}
        .portal-product-kpi--critical{border-block-start-color:var(--error,#da1e28)}
        .portal-product-kpi strong{font-size:1.25rem;font-weight:400;line-height:1.2}
        .portal-product-kpi span{font-size:.75rem;color:var(--text-secondary)}
        .portal-product-panel .portal-filing-board{margin:0 0 1rem}
        .portal-product-panel .portal-filing-board .portal-lead{display:none}
        .portal-product-panel .portal-filing-grid{grid-template-columns:1fr 1fr}
        .portal-product-chart{margin:0}
        .portal-product-chart .carbon-insight{margin:0;padding:0;border:0}
        .portal-product-footer{margin:1rem 0 0;font-size:.875rem}
        .portal-findings{list-style:none;margin:0;padding:0}
        .portal-findings__item{margin:0;border-block-end:1px solid var(--border-subtle)}
        .portal-findings__item:last-child{border-block-end:0}
        .portal-findings__item a{display:flex;align-items:flex-start;gap:.75rem;padding:.875rem 0;text-decoration:none;color:var(--text)}
        .portal-findings__item a:hover{background:transparent;color:var(--link)}
        .portal-findings__item .carbon-icon{flex:0 0 auto;margin-block-start:.15rem;fill:var(--link)}
        .portal-findings__item--critical .carbon-icon{fill:var(--error)}
        .portal-findings__item--attention .carbon-icon{fill:var(--warning-icon)}
        .portal-findings__copy{display:flex;flex-direction:column;gap:.2rem;min-inline-size:0}
        .portal-findings__copy strong{font-size:.875rem;font-weight:600;overflow-wrap:anywhere}
        .portal-findings__detail{font-size:.75rem;color:var(--text-secondary);display:flex;flex-wrap:wrap;gap:.35rem .75rem}
        .portal-findings__status{color:var(--error);font-weight:600}
        .portal-kanban-add .cds--form-item{margin:0}
        .tablewrap,.portal-sheet-wrap{display:flex;flex-direction:column;margin:1rem 0;border:1px solid var(--border-subtle);background:var(--layer);max-block-size:min(70vh,52rem)}
        .portal-sheet-scroll{overflow:auto;flex:1;min-block-size:0}
        .carbon-table-toolbar{display:flex;align-items:center;gap:.75rem;flex-wrap:wrap;padding:.5rem .75rem;border-block-end:1px solid var(--border-subtle);background:var(--layer);flex:0 0 auto}
        .carbon-table-toolbar input{flex:1;min-inline-size:10rem;max-inline-size:24rem;min-block-size:2rem;padding:.25rem .5rem;font-size:.8125rem}
        .carbon-table-pages{display:flex;align-items:center;justify-content:flex-end;gap:.75rem;padding:.35rem .75rem;border-block-start:1px solid var(--border-subtle);flex-wrap:wrap;font-size:.75rem;background:var(--layer);flex:0 0 auto}
        .carbon-table-pages label{display:flex;align-items:center;gap:.35rem;margin:0;font-size:.75rem}
        .carbon-table-pages select{width:auto;min-width:3.5rem;min-block-size:1.75rem;padding:.15rem .35rem;font-size:.75rem}
        .carbon-table-pages .cds--btn{min-block-size:1.75rem;padding:.15rem .5rem;font-size:.75rem}
        table.cds--data-table,table.portal-sheet{width:100%;border-collapse:collapse;table-layout:auto;font-size:.8125rem;line-height:1.25;font-variant-numeric:tabular-nums}
        table.cds--data-table th,table.cds--data-table td,table.portal-sheet th,table.portal-sheet td{border:1px solid var(--border-subtle);padding:.28rem .5rem;vertical-align:middle;text-align:start;white-space:nowrap;min-inline-size:3.5rem;max-inline-size:18rem;overflow:hidden;text-overflow:ellipsis}
        table.cds--data-table th,table.portal-sheet th{background:var(--layer-hover);font-weight:600;font-size:.6875rem;letter-spacing:.02em;text-transform:uppercase;color:var(--text-secondary);position:sticky;inset-block-start:0;z-index:1}
        table.cds--data-table tbody tr:nth-child(even),table.portal-sheet tbody tr:nth-child(even){background:color-mix(in srgb,var(--layer-hover) 45%,transparent)}
        table.cds--data-table tbody tr:hover,table.portal-sheet tbody tr:hover{background:var(--layer-hover)}
        table.cds--data-table td a,table.portal-sheet td a{font-weight:500}
        table.cds--data-table td form,table.portal-sheet td form{margin:0;display:inline}
        table.cds--data-table td button,table.portal-sheet td button{min-block-size:1.5rem;padding:.1rem .4rem;font-size:.75rem}
        .portal-sheet-num{text-align:end!important;font-variant-numeric:tabular-nums}
        .portal-sheet-empty{white-space:normal!important;color:var(--text-secondary);padding:.75rem!important;max-inline-size:none!important}
        .carbon-table-caption{caption-side:top;text-align:left;padding:.4rem .75rem;font-weight:600;font-size:.8125rem;color:var(--text);border-block-end:1px solid var(--border-subtle);background:var(--layer)}
        .carbon-sort{background:transparent!important;color:inherit!important;padding:0;min-height:1.25rem;text-align:left;display:inline-flex;gap:.25rem;align-items:center;font:inherit;font-weight:600;text-transform:uppercase;letter-spacing:.02em;inline-size:100%}
        .carbon-sort .carbon-icon{width:12px;height:12px;opacity:.7;flex:0 0 auto}
        .dimensions{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1rem}
        cds-breadcrumb{display:block;margin:0 0 1rem}
        @media(max-width:66rem){.portal-form-grid{grid-template-columns:1fr}.dimensions{grid-template-columns:repeat(2,minmax(0,1fr))}.portal-filing-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.carbon-kpis,.portal-product-kpis,.carbon-tile-grid--small{grid-template-columns:repeat(5,minmax(0,1fr))}.portal-tiles,.carbon-tile-grid--medium,.portal-product-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.portal-kanban-column{min-block-size:12rem}.portal-kanban-add{grid-template-columns:1fr}}
        @media(max-width:42rem){
          .portal-side-nav{transform:translateX(-100%)!important;inline-size:var(--side-expanded)!important}
          .portal-side-nav[data-state=expanded]{transform:none!important}
          .portal-side-nav .portal-nav-title,.portal-side-nav .cds--side-nav__link-text{display:block}
          .portal-side-nav a.cds--side-nav__link{justify-content:flex-start;padding-inline:1rem!important}
          .portal-body--app .cds--content,.portal-body--app.portal-nav-open .cds--content{margin-inline-start:0}
          .cds--header .cds--header__nav{display:none!important}
          .portal-masthead{flex-direction:column;gap:.75rem}
          .portal-action-label{display:none}
          .dimensions{grid-template-columns:1fr}
          .portal-filing-grid,.carbon-kpis{grid-template-columns:1fr}
          .portal-product-header{flex-direction:column;align-items:stretch}
          .portal-product-actions__menu{inset-inline:0;min-inline-size:0}
        }
    """.trimIndent()
    val js: String
        get() {
            val asleep = asleepSvg()
            val awake = awakeSvg()
            return """
(()=>{'use strict';
const media=window.matchMedia('(prefers-color-scheme: dark)');
let mode='system';
try{const saved=localStorage.getItem('archiman-theme');if(['system','light','dark'].includes(saved))mode=saved;}catch(e){}
const effective=()=>mode==='system'?(media.matches?'dark':'light'):mode;
const apply=()=>{document.documentElement.dataset.theme=effective();paintThemeToggle();};
const paintThemeToggle=()=>{
  const btn=document.getElementById('theme-toggle');if(!btn)return;
  const dark=effective()==='dark';
  btn.setAttribute('aria-pressed',String(dark));
  btn.setAttribute('aria-label',dark?'Switch to light theme':'Switch to dark theme');
  btn.title=btn.getAttribute('aria-label');
  btn.innerHTML=dark?$asleep:$awake;
};
if(media.addEventListener)media.addEventListener('change',()=>{if(mode==='system')apply();});
else media.addListener(()=>{if(mode==='system')apply();});
apply();

const mountTheme=()=>{
  const btn=document.getElementById('theme-toggle');if(!btn||btn.dataset.bound)return;btn.dataset.bound='1';
  btn.addEventListener('click',()=>{
    mode=effective()==='dark'?'light':'dark';
    try{localStorage.setItem('archiman-theme',mode);}catch(e){}
    apply();
  });
};

const side=()=>document.getElementById('portal-side-nav');
const setNav=(state)=>{
  const nav=side();if(!nav)return;
  nav.dataset.state=state;
  document.body.classList.toggle('portal-nav-open',state==='expanded');
  const toggle=document.getElementById('portal-nav-toggle');
  if(toggle)toggle.setAttribute('aria-expanded',String(state==='expanded'));
  try{localStorage.setItem('archiman-nav',state);}catch(e){}
};
const mountNav=()=>{
  const nav=side();if(!nav||nav.dataset.bound)return;nav.dataset.bound='1';
  let saved='rail';
  try{saved=localStorage.getItem('archiman-nav')||'rail';}catch(e){}
  if(window.matchMedia('(max-width:42rem)').matches)saved='rail';
  setNav(saved==='expanded'?'expanded':'rail');
  let leaveTimer=0;let hold=false;
  const toggle=document.getElementById('portal-nav-toggle');
  if(toggle&&!toggle.dataset.bound){toggle.dataset.bound='1';toggle.addEventListener('click',()=>setNav(nav.dataset.state==='expanded'?'rail':'expanded'));}
  nav.addEventListener('mouseenter',()=>{if(window.matchMedia('(max-width:42rem)').matches)return;window.clearTimeout(leaveTimer);setNav('expanded');});
  nav.addEventListener('mouseleave',()=>{if(window.matchMedia('(max-width:42rem)').matches||hold)return;leaveTimer=window.setTimeout(()=>setNav('rail'),220);});
  nav.addEventListener('pointerdown',()=>{hold=true;nav.classList.add('portal-side-nav--hold');});
  window.addEventListener('pointerup',()=>{hold=false;nav.classList.remove('portal-side-nav--hold');});
  nav.querySelectorAll('a.cds--side-nav__link').forEach(link=>{
    link.addEventListener('click',event=>{
      /* Ensure navigation always proceeds even if a parent handler interferes. */
      if(link.getAttribute('href')){/* allow default */}
    });
  });
  document.getElementById('content')?.addEventListener('click',()=>{if(window.matchMedia('(max-width:42rem)').matches)setNav('rail');});
};

const mountSearch=()=>{
  const root=document.getElementById('portal-search');if(!root)return;
  const openBtn=document.getElementById('portal-search-open');
  const closeBtn=document.getElementById('portal-search-close');
  const input=document.getElementById('portal-search-input');
  const results=document.getElementById('portal-search-results');
  const index=[];
  document.querySelectorAll('.cds--side-nav__link, .cds--header__menu-item').forEach(a=>{
    index.push({label:(a.getAttribute('data-nav-label')||a.textContent).trim(),href:a.getAttribute('href'),group:a.closest('[data-nav-group]')?.getAttribute('data-nav-group')||'Navigation'});
  });
  document.querySelectorAll('a.portal-tile, a.cds--tile--clickable').forEach(a=>{
    index.push({label:(a.querySelector('h2')?.textContent||a.textContent).trim(),href:a.getAttribute('href'),group:'Page'});
  });
  document.querySelectorAll('main h1, main h2').forEach(h=>{
    index.push({label:h.textContent.trim(),href:'#content',group:'On this page'});
  });
  const escapeHtml=(value)=>String(value).replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  const render=(query)=>{
    const q=query.trim().toLowerCase();
    const hits=(!q?index.slice(0,8):index.filter(item=>item.label.toLowerCase().includes(q))).slice(0,12);
    results.innerHTML=hits.length?hits.map(item=>'<li role="option"><a href="'+item.href+'"><strong>'+escapeHtml(item.label)+'</strong><small>'+escapeHtml(item.group)+'</small></a></li>').join(''):'<li class="portal-search__empty">No matching pages</li>';
  };
  const open=()=>{root.hidden=false;input.value='';render('');input.focus();};
  const close=()=>{root.hidden=true;};
  openBtn?.addEventListener('click',open);
  closeBtn?.addEventListener('click',close);
  root.addEventListener('click',event=>{if(event.target===root)close();});
  input?.addEventListener('input',()=>render(input.value));
  document.addEventListener('keydown',event=>{
    const tag=document.activeElement?.tagName;
    if((event.key==='/'||((event.ctrlKey||event.metaKey)&&event.key.toLowerCase()==='k'))&&!['INPUT','TEXTAREA','SELECT'].includes(tag)){event.preventDefault();open();}
    if(event.key==='Escape'&&!root.hidden)close();
  });
};

const mountForms=()=>{
  for(const form of document.querySelectorAll('form')){
    const action=form.querySelector('input[name=action]')?.value||'';
    if(/Delete${'$'}|Remove${'$'}|Void${'$'}/.test(action)||action==='archiveItem'){for(const button of form.querySelectorAll('button'))button.classList.add('danger');}
  }
};

const boot=()=>{mountTheme();mountNav();mountSearch();mountForms();};
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
})();
            """.trimIndent()
        }

    private fun asleepSvg() = jsLiteral(CarbonPortalIcons.svg("asleep"))
    private fun awakeSvg() = jsLiteral(CarbonPortalIcons.svg("awake"))
    private fun jsLiteral(html: String) = "'" + html.replace("\\", "\\\\").replace("'", "\\'") + "'"
}
