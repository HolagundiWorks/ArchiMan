package com.holagundi.archiman.portal

/** Carbon UI shell shared by sign-in, projects, transfer and the workspace. */
object CarbonPortal {
    enum class Audience { SIGNED_OUT, ADMIN, TRANSFER }

    fun page(
        title: String,
        body: String,
        audience: Audience,
        current: String = "",
        crumb: String? = null,
        workspace: Boolean = false,
        csrf: String? = null,
        brand: PortalBrand = PortalBrand.FALLBACK,
    ): String {
        val home = when (audience) {
            Audience.TRANSFER -> "/transfer"
            Audience.ADMIN -> "/workspace?section=dashboard"
            Audience.SIGNED_OUT -> "/"
        }
        val practice = brand.displayName
        val brandMark = if (brand.hasLogo) {
            """<img class="portal-brand-logo" src="${PortalHttp.escape(brand.logoDataUrl)}" alt="" width="28" height="28">"""
        } else {
            CarbonPortalIcons.svg("architecture", "portal-brand-icon")
        }
        val groups = navGroups(audience)
        val flat = groups.flatMap { it.items }
        val headerLinks = flat.joinToString("") { link ->
            val currentAttr = if (link.id == current) " aria-current=\"page\"" else ""
            "<li><a class=\"cds--header__menu-item\" href=\"${link.href}\"$currentAttr>${CarbonPortalIcons.svg(link.icon)}<span>${PortalHttp.escape(link.label)}</span></a></li>"
        }
        val side = if (groups.isEmpty()) "" else {
            val items = groups.joinToString("") { group ->
                val links = group.items.joinToString("") { link ->
                    val currentClass = if (link.id == current) " cds--side-nav__link--current" else ""
                    val currentAttr = if (link.id == current) " aria-current=\"page\"" else ""
                    """<li class="cds--side-nav__item"><a class="cds--side-nav__link$currentClass" href="${link.href}"$currentAttr title="${PortalHttp.escape(link.label)}" data-nav-label="${PortalHttp.escape(link.label)}">${CarbonPortalIcons.svg(link.icon, "cds--side-nav__icon")}<span class="cds--side-nav__link-text">${PortalHttp.escape(link.label)}</span></a></li>"""
                }
                """<li class="cds--side-nav__item portal-nav-group" data-nav-group="${PortalHttp.escape(group.title)}"><div class="portal-nav-title" aria-hidden="true">${PortalHttp.escape(group.title)}</div><ul class="portal-nav-menu">$links</ul></li>"""
            }
            """
            <aside class="cds--side-nav cds--side-nav--ux portal-side-nav" id="portal-side-nav" data-state="rail" aria-label="Portal">
              <div class="portal-side-brand" title="${PortalHttp.escape(practice)}">
                $brandMark
                <div class="portal-side-brand__copy">
                  <strong>${PortalHttp.escape(practice)}</strong>
                  <span>MynaHub portal</span>
                </div>
              </div>
              <nav class="cds--side-nav__navigation" aria-label="Portal">
                <ul class="cds--side-nav__items portal-nav-items">$items</ul>
              </nav>
            </aside>
            """.trimIndent()
        }
        val chrome = if (audience == Audience.SIGNED_OUT) {
            """
            <div class="cds--header__global">
              <button type="button" class="cds--header__action portal-theme-toggle" id="theme-toggle" aria-label="Toggle color theme" aria-pressed="false">${CarbonPortalIcons.svg("asleep")}</button>
            </div>
            """.trimIndent()
        } else {
            val signOut = if (csrf.isNullOrBlank()) "" else """
            <form class="portal-sign-out" method="post" action="/logout">
              <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
              <button class="cds--btn cds--btn--ghost cds--header__action" type="submit" aria-label="Sign out">${CarbonPortalIcons.svg("logout")}<span class="portal-action-label">Sign out</span></button>
            </form>
            """.trimIndent()
            """
            <div class="cds--header__global">
              <button type="button" class="cds--header__action" id="portal-search-open" aria-label="Open search" aria-haspopup="dialog" aria-controls="portal-search">${CarbonPortalIcons.svg("search")}</button>
              <button type="button" class="cds--header__action portal-theme-toggle" id="theme-toggle" aria-label="Toggle color theme" aria-pressed="false">${CarbonPortalIcons.svg("asleep")}</button>
              $signOut
            </div>
            """.trimIndent()
        }
        val menuToggle = if (groups.isEmpty()) "" else """
            <button type="button" class="cds--header__menu-toggle cds--header__action" id="portal-nav-toggle" aria-label="Toggle navigation" aria-controls="portal-side-nav" aria-expanded="false">${CarbonPortalIcons.svg("menu")}</button>
        """.trimIndent()
        val search = if (audience == Audience.SIGNED_OUT) "" else """
            <div class="portal-search" id="portal-search" hidden>
              <div class="portal-search__panel" role="dialog" aria-modal="true" aria-label="Search portal">
                <div class="portal-search__bar">
                  ${CarbonPortalIcons.svg("search", "portal-search__glyph")}
                  <input class="cds--text-input portal-search__input" id="portal-search-input" type="search" placeholder="Search pages, projects and headings" autocomplete="off" aria-label="Search portal">
                  <button type="button" class="cds--btn cds--btn--ghost portal-search__close" id="portal-search-close" aria-label="Close search">${CarbonPortalIcons.svg("close")}</button>
                </div>
                <ul class="portal-search__results" id="portal-search-results" role="listbox" aria-label="Search results"></ul>
                <p class="portal-search__hint">Press <kbd>Esc</kbd> to close · <kbd>/</kbd> or <kbd>Ctrl</kbd>+<kbd>K</kbd> to open</p>
              </div>
            </div>
        """.trimIndent()
        val breadcrumb = crumb?.let {
            """<cds-breadcrumb aria-label="Breadcrumb"><cds-breadcrumb-item><cds-breadcrumb-link href="$home">${PortalHttp.escape(practice)}</cds-breadcrumb-link></cds-breadcrumb-item><cds-breadcrumb-item aria-current="page">${PortalHttp.escape(it)}</cds-breadcrumb-item></cds-breadcrumb>"""
        }.orEmpty()
        val extra = if (workspace) """<link rel="stylesheet" href="/workspace.css"><script src="/workspace.js" defer></script>""" else ""
        val accent = brand.safeAccent
        val titleBrand = if (practice == "Practice") "MynaHub" else practice
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>${PortalHttp.escape(title)} · ${PortalHttp.escape(titleBrand)}</title>
              <link rel="stylesheet" href="/theme.css">
              <link rel="stylesheet" href="/carbon/vendor.css">
              $extra
              <style>:root,:root[data-theme=dark]{--brand-accent:$accent;--link:$accent;--interactive:$accent;--focus:$accent;--button-primary:$accent}</style>
              <script src="/theme.js"></script>
              <script src="/carbon/vendor.js" defer></script>
            </head>
            <body class="portal-body${if (audience == Audience.SIGNED_OUT) " portal-body--auth" else " portal-body--app"}" data-practice="${PortalHttp.escape(practice)}">
              <a class="carbon-skip" href="#content">Skip to content</a>
              <header class="cds--header" aria-label="${PortalHttp.escape(practice)} · MynaHub">
                $menuToggle
                <a class="cds--header__name" href="$home" title="${PortalHttp.escape(practice)}">$brandMark<span class="cds--header__name--prefix">MynaHub</span><span class="portal-brand-product">${PortalHttp.escape(practice)}</span></a>
                <nav class="cds--header__nav" aria-label="Portal"><ul class="cds--header__menu-bar">$headerLinks</ul></nav>
                $chrome
              </header>
              $side
              $search
              <main class="cds--content" id="content"><div class="portal-canvas">$breadcrumb$body</div></main>
            </body>
            </html>
        """.trimIndent()
    }

    fun textField(id: String, name: String, label: String, type: String = "text", attributes: String = ""): String = """
        <div class="cds--form-item">
          <label class="cds--label" for="$id">${PortalHttp.escape(label)}</label>
          <input class="cds--text-input" id="$id" name="$name" type="$type" $attributes>
        </div>
    """.trimIndent()

    fun button(label: String, kind: String = "primary", icon: String = "arrow") =
        """<button class="cds--btn cds--btn--$kind${if (kind == "danger") " danger" else ""}" type="submit">${CarbonPortalIcons.svg(icon)}<span>${PortalHttp.escape(label)}</span></button>"""

    fun tile(href: String, title: String, meta: String, icon: String, alert: String = "ok"): String {
        val tone = when (alert) {
            "critical" -> "portal-tile--critical"
            "warning" -> "portal-tile--warning"
            "none" -> ""
            else -> "portal-tile--ok"
        }
        return """
        <a class="cds--tile cds--tile--clickable portal-tile $tone" href="$href">
          <span class="portal-tile-icon">${CarbonPortalIcons.svg(icon, "portal-tile-glyph")}</span>
          <span class="portal-tile-copy">
            <h2>${PortalHttp.escape(title)}</h2>
            <p class="portal-tile-meta">${PortalHttp.escape(meta)}</p>
          </span>
          ${CarbonPortalIcons.svg("arrow", "portal-tile-arrow")}
        </a>
    """.trimIndent()
    }

    fun masthead(title: String, lead: String, icon: String): String = """
        <header class="portal-masthead">
          <span class="portal-masthead-icon">${CarbonPortalIcons.svg(icon, "portal-masthead-glyph")}</span>
          <div class="portal-masthead-copy">
            <h1>${PortalHttp.escape(title)}</h1>
            <p class="portal-lead">${PortalHttp.escape(lead)}</p>
          </div>
        </header>
    """.trimIndent()

    fun formSection(title: String, body: String, hint: String = ""): String {
        val hintHtml = if (hint.isBlank()) "" else """<p class="portal-form-hint">${PortalHttp.escape(hint)}</p>"""
        return """<fieldset class="portal-form-section"><legend class="portal-form-legend">${PortalHttp.escape(title)}</legend>$hintHtml<div class="portal-form-grid">$body</div></fieldset>"""
    }

    private fun navGroups(audience: Audience): List<NavGroup> = when (audience) {
        Audience.SIGNED_OUT -> emptyList()
        Audience.TRANSFER -> listOf(
            NavGroup("Data", listOf(NavLink("transfer", "/transfer", "Practice transfer", "backup"))),
        )
        Audience.ADMIN -> listOf(
            NavGroup(
                "Practice",
                listOf(
                    NavLink("projects", "/", "Projects", "building"),
                    NavLink("dashboard", "/workspace?section=dashboard", "Overview", "dashboard"),
                    NavLink("tasks", "/workspace?section=tasks", "Tasks", "task"),
                ),
            ),
            NavGroup(
                "People",
                listOf(
                    NavLink("clients", "/workspace?section=contacts&type=client", "Clients", "users"),
                    NavLink("contractors", "/workspace?section=contacts&type=contractor", "Contractors", "construction"),
                ),
            ),
            NavGroup(
                "Catalogue",
                listOf(
                    NavLink("items", "/workspace?section=items", "Item library", "ruler"),
                    NavLink("documents", "/workspace?section=documents", "Documents", "folder"),
                ),
            ),
            NavGroup(
                "Company",
                listOf(
                    NavLink("company", "/workspace?section=company", "Letterhead", "enterprise"),
                ),
            ),
            NavGroup(
                "Data",
                listOf(
                    NavLink("transfer", "/transfer", "Practice transfer", "backup"),
                ),
            ),
        )
    }

    private data class NavLink(val id: String, val href: String, val label: String, val icon: String)
    private data class NavGroup(val title: String, val items: List<NavLink>)
}
