package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log

import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.view.View
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentDeveloperOptionsLogBinding
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsDeveloperOptionsLogFragment: BaseFragment<FragmentDeveloperOptionsLogBinding>(FragmentDeveloperOptionsLogBinding::class) {

    private val viewModel by viewModel<SettingsDeveloperOptionsLogViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.developerOptionsLogContent){
            text = Html.fromHtml(getString(R.string.developer_options_log_content), Html.FROM_HTML_MODE_COMPACT)
            Linkify.addLinks(this, Linkify.ALL)
            movementMethod = BetterLinkMovementMethod.newInstance().setOnLinkClickListener { _, url ->
                viewModel.onContentsClicked()
                true
            }
        }
        binding.developerOptionsLogButton.setOnClickListener {
            viewModel.onStartDumpClicked()
        }
    }

}