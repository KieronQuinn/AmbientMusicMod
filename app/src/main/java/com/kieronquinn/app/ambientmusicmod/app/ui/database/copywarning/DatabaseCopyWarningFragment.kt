package com.kieronquinn.app.ambientmusicmod.app.ui.database.copywarning

import android.os.Bundle
import android.view.View
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentDatabaseCopyWarningBinding
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applySystemWindowInsetsPadding
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseCopyWarningFragment: BaseFragment<FragmentDatabaseCopyWarningBinding>(FragmentDatabaseCopyWarningBinding::class) {

    private val viewModel by viewModel<DatabaseCopyWarningViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applySystemWindowInsetsPadding(bottom = true)
        binding.databaseViewerCopyWarningStart.setOnClickListener {
            viewModel.onCopyClicked()
        }
    }

}