package com.holagundi.archiman.portal

import com.holagundi.archiman.data.BackupArchive
import com.holagundi.archiman.domain.PasswordEnvelope

/** Practice backup download and restore over the Wi‑Fi portal, including before the admin workspace is opened. */
object PortalTransfer {
    const val ROLE = "Transfer"

    fun allowed(role: String?) = role == "Admin" || role == ROLE

    /** No admin exists yet: the access code alone may open restore/download transfer. */
    fun codeOnly(codeOk: Boolean, adminCount: Int) = codeOk && adminCount == 0

    fun page(practice: String, csrf: String, error: String?): String {
        val notice = error?.let { """<p class="notice" role="alert">${PortalHttp.escape(it)}</p>""" }.orEmpty()
        return """
            ${CarbonPortal.masthead("Practice transfer", "${practice.ifBlank { "Practice on this phone" }}. Save the practice on this computer, or replace the practice on the phone. The backup password cannot be recovered.", "backup")}
            $notice
            <div class="portal-tiles">
              <section class="cds--tile portal-panel">
                <h2>${CarbonPortalIcons.svg("download")}<span>Download backup</span></h2>
                <p class="portal-tile-meta">Password-protect the file before it leaves this computer.</p>
                <form method="post" action="/transfer/backup">
                  <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
                  ${CarbonPortal.textField("backup-password", "password", "Backup password (12–256 characters)", "password", "minlength=\"12\" maxlength=\"256\" autocomplete=\"new-password\" required")}
                  ${CarbonPortal.textField("backup-confirmation", "confirmation", "Confirm password", "password", "minlength=\"12\" maxlength=\"256\" autocomplete=\"new-password\" required")}
                  ${CarbonPortal.button("Download practice backup", icon = "download")}
                </form>
              </section>
              <section class="cds--tile portal-panel">
                <h2>${CarbonPortalIcons.svg("upload")}<span>Restore from this computer</span></h2>
                <p class="warning" role="status" data-kind="warning">This replaces every practice record on the phone. Type REPLACE to confirm.</p>
                <form method="post" action="/transfer/restore" enctype="multipart/form-data">
                  <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
                  <div class="cds--form-item">
                    <label class="cds--label" for="backup-file">Backup file</label>
                    <input class="portal-file" id="backup-file" name="file" type="file" required>
                  </div>
                  ${CarbonPortal.textField("restore-password", "password", "Backup password", "password", "maxlength=\"256\" autocomplete=\"current-password\"")}
                  ${CarbonPortal.textField("restore-confirm", "confirm", "Type REPLACE", attributes = "autocomplete=\"off\" required")}
                  ${CarbonPortal.button("Replace practice on the phone", "danger", icon = "upload")}
                </form>
              </section>
            </div>
        """.trimIndent()
    }

    fun protect(plain: ByteArray, password: String, confirmation: String): ByteArray {
        require(password.length in 12..256 && password == confirmation) { "Use a matching password of 12–256 characters." }
        val secret = password.toCharArray()
        return try {
            PasswordEnvelope.encrypt(plain, secret, BackupArchive.safeByteLimit())
        } finally {
            secret.fill('\u0000')
        }
    }

    fun open(file: ByteArray, password: String, confirmation: String): ByteArray {
        require(confirmation == "REPLACE") { "Type REPLACE to confirm that this replaces the practice on the phone." }
        require(file.isNotEmpty() && file.size <= BackupArchive.safeByteLimit() + 128) { "That backup is empty or larger than this phone can restore." }
        if (!PasswordEnvelope.isEncrypted(file)) return file
        require(password.isNotEmpty()) { "Enter the backup password." }
        val secret = password.toCharArray()
        return try {
            PasswordEnvelope.decrypt(file, secret, BackupArchive.safeByteLimit())
        } finally {
            secret.fill('\u0000')
        }
    }
}
