package com.holagundi.archiman.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun shareCsv(context: Context, fileName: String, csv: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export measurement sheet").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
