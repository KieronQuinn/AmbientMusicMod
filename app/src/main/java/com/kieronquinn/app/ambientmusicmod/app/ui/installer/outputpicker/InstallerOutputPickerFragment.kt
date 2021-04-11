package com.kieronquinn.app.ambientmusicmod.app.ui.installer.outputpicker

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentInstallerOutputPickerBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallerOutputPickerFragment: BaseFragment<FragmentInstallerOutputPickerBinding>(FragmentInstallerOutputPickerBinding::class) {

    private val viewModel by viewModel<InstallerOutputPickerViewModel>()

    private val documentPicker = registerForActivityResult(ActivityResultContracts.CreateDocument()){
        if(it != null) {
            viewModel.onOutputPicked(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.installerOutputPickerButton.setOnClickListener {
            viewModel.onChooseClicked(documentPicker)
        }
    }

}