package com.ilya.safarshots.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

object ShareUtils {
    fun shareImageIntent(context: Context, uriString: String, title: String = "Share image"): Intent {
        val uri = shareableUri(context, uriString)
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            title,
        )
    }

    private fun shareableUri(context: Context, uriString: String): Uri {
        val uri = uriString.toUri()
        if (uri.scheme != "file") return uri
        val file = File(uri.path.orEmpty())
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
