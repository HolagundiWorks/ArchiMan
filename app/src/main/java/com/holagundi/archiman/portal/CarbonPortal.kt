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
    ): String {
        val home = if (audience == Audience.TRANSFER) "/transfer" else "/"
        val links = links(audience)
        val headerLinks = links.joinToString("") { (id, href, label, icon) ->
            val currentAttr = if (id == current) " aria-current=\"page\"" else ""
            "<li><a class=\"cds--header__menu-item\" href=\"$href\"$currentAttr>${CarbonPortalIcons.svg(icon)}<span>${PortalHttp.escape(label)}</span></a></li>"
        }
        val side = if (links.isEmpty()) "" else {
            val items = links.joinToString("") { (id, href, label, icon) ->
                val currentClass = if (id == current) " cds--side-nav__link--current" else ""
                val currentAttr = if (id == current) " aria-current=\"page\"" else ""
                """<li class="cds--side-nav__item"><a class="cds--side-nav__link$currentClass" href="$href"$currentAttr>${CarbonPortalIcons.svg(icon, "cds--side-nav__icon")}<span class="cds--side-nav__link-text">${PortalHttp.escape(label)}</span></a></li>"""
            }
            """<aside class="cds--side-nav cds--side-nav--expanded" aria-label="Portal"><nav class="cds--side-nav__navigation" aria-label="Portal"><ul class="cds--side-nav__items">$items</ul></nav></aside>"""
        }
        val signOut = if (csrf.isNullOrBlank()) "" else """
            <form class="portal-sign-out" method="post" action="/logout">
              <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
              <button class="cds--btn cds--btn--ghost" type="submit">${CarbonPortalIcons.svg("logout")}<span>Sign out</span></button>
            </form>
        """.trimIndent()
        val breadcrumb = crumb?.let {
            """<cds-breadcrumb aria-label="Breadcrumb"><cds-breadcrumb-item><cds-breadcrumb-link href="$home">Practice</cds-breadcrumb-link></cds-breadcrumb-item><cds-breadcrumb-item aria-current="page">${PortalHttp.escape(it)}</cds-breadcrumb-item></cds-breadcrumb>"""
        }.orEmpty()
        val extra = if (workspace) """<link rel="stylesheet" href="/workspace.css"><script src="/workspace.js" defer></script>""" else ""
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>${PortalHttp.escape(title)} · ArchiMan</title>
              <link rel="stylesheet" href="/theme.css">
              <link rel="stylesheet" href="/carbon/vendor.css">
              $extra
              <script src="/theme.js"></script>
              <script src="/carbon/vendor.js" defer></script>
            </head>
            <body class="portal-body${if (audience == Audience.SIGNED_OUT) " portal-body--auth" else ""}">
              <a class="carbon-skip" href="#content">Skip to content</a>
              <header class="cds--header" aria-label="ArchiMan">
                <a class="cds--header__name" href="$home">${CarbonPortalIcons.svg("architecture", "portal-brand-icon")}<span class="cds--header__name--prefix">ArchiMan</span>&nbsp;Practice</a>
                <nav class="cds--header__nav" aria-label="Portal"><ul class="cds--header__menu-bar">$headerLinks</ul></nav>
                <div class="cds--header__global">$signOut</div>
              </header>
              $side
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

    fun tile(href: String, title: String, meta: String, icon: String): String = """
        <a class="cds--tile cds--tile--clickable portal-tile" href="$href">
          <span class="portal-tile-icon">${CarbonPortalIcons.svg(icon, "portal-tile-glyph")}</span>
          <span class="portal-tile-copy">
            <h2>${PortalHttp.escape(title)}</h2>
            <p class="portal-tile-meta">${PortalHttp.escape(meta)}</p>
          </span>
          ${CarbonPortalIcons.svg("arrow", "portal-tile-arrow")}
        </a>
    """.trimIndent()

    fun masthead(title: String, lead: String, icon: String): String = """
        <header class="portal-masthead">
          <span class="portal-masthead-icon">${CarbonPortalIcons.svg(icon, "portal-masthead-glyph")}</span>
          <div>
            <h1>${PortalHttp.escape(title)}</h1>
            <p class="portal-lead">${PortalHttp.escape(lead)}</p>
          </div>
        </header>
    """.trimIndent()

    private fun links(audience: Audience): List<NavLink> = when (audience) {
        Audience.SIGNED_OUT -> emptyList()
        Audience.TRANSFER -> listOf(NavLink("transfer", "/transfer", "Practice transfer", "backup"))
        Audience.ADMIN -> listOf(
            NavLink("projects", "/", "Projects", "building"),
            NavLink("dashboard", "/workspace?section=dashboard", "Overview", "dashboard"),
            NavLink("company", "/workspace?section=company", "Company & letterhead", "enterprise"),
            NavLink("items", "/workspace?section=items", "Item library", "ruler"),
            NavLink("clients", "/workspace?section=contacts&type=client", "Clients", "users"),
            NavLink("contractors", "/workspace?section=contacts&type=contractor", "Contractors", "construction"),
            NavLink("documents", "/workspace?section=documents", "Documents", "folder"),
            NavLink("transfer", "/transfer", "Practice transfer", "backup"),
        )
    }

    private data class NavLink(val id: String, val href: String, val label: String, val icon: String)
}
