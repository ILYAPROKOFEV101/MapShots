package com.ilya.safarshots.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.ilya.safarshots.R
import java.io.File

object ShareUtils {
    /**
     * Builds an [Intent.createChooser] for sharing an image. [title] is shown as the
     * chooser header; callers should pass a localized string (e.g.
     * `R.string.share_image_title`) so it matches the rest of the UI. The default
     * value lazily resolves the string through [context], which already carries the
     * active per-app locale.
     */
    fun shareImageIntent(
        context: Context,
        uriString: String,
        title: CharSequence = context.getString(R.string.share_image_title),
    ): Intent {
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
