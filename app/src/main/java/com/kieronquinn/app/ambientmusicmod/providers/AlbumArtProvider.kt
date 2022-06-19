package com.kieronquinn.app.ambientmusicmod.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.squareup.picasso.Picasso

class AlbumArtProvider: ContentProvider() {

    companion object {
        private const val EXTRA_KEY_BITMAP = "bitmap"
    }

    private val picasso by lazy {
        Picasso.Builder(context!!).build()
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(youtubeId: String, arg: String?, extras: Bundle?): Bundle? {
        val bitmap = try {
            picasso.load("https://img.youtube.com/vi/$youtubeId/maxresdefault.jpg").get()
        }catch (e: Exception){
            null
        } ?: return null
        return bundleOf(EXTRA_KEY_BITMAP to bitmap)
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        throw RuntimeException("Unsupported")
    }

    override fun getType(p0: Uri): String? {
        throw RuntimeException("Unsupported")
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        throw RuntimeException("Unsupported")
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        throw RuntimeException("Unsupported")
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        throw RuntimeException("Unsupported")
    }


}