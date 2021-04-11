package com.kieronquinn.app.ambientmusicmod.app.ui.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.format.DateFormat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.components.github.UpdateChecker
import com.kieronquinn.app.ambientmusicmod.app.ui.update.download.UpdateDownloadBottomSheetViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.time.Instant
import java.util.*

class UpdateBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val updateViewModel by sharedViewModel<UpdateDownloadBottomSheetViewModel>()
    private val updateChecker by inject<UpdateChecker>()
    private val navArgs by navArgs<UpdateBottomSheetFragmentArgs>()

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        title(R.string.bs_update_title)
        val update = navArgs.update
        val timestamp = DateFormat.getDateFormat(view.context).format(Date.from(Instant.parse(update.timestamp)))
        val message = Html.fromHtml(
            getString(
                R.string.bs_update_content,
                BuildConfig.VERSION_NAME,
                update.name,
                timestamp,
                update.changelog.formatChangelog()
            ),
            Html.FROM_HTML_MODE_COMPACT)
        message(text = message)
        noAutoDismiss()
        positiveButton(R.string.bs_update_download){
            updateChecker.hasDismissedDialog = true
            if(update.assetUrl.endsWith(".apk")) {
                updateViewModel.startDownload(this@UpdateBottomSheetFragment, update.assetUrl, update.assetName)
            }else{
                launchUrl(update.releaseUrl)
            }
        }
        negativeButton(R.string.close){
            updateChecker.hasDismissedDialog = true
            findNavController().navigateUp()
        }
        neutralButton(R.string.bs_update_download_github){
            launchUrl(update.releaseUrl)
            updateChecker.hasDismissedDialog = true
            findNavController().navigateUp()
        }
    }

    private fun String.formatChangelog(): String {
        return this.replace("\n", "<br>")
    }

    private fun launchUrl(url: String){
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        })
    }

}