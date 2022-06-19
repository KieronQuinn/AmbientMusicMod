package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.batteryoptimisation

import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.screens.batteryoptimisation.BatteryOptimisationFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupBatteryOptimisationFragment: BatteryOptimisationFragment(), BackAvailable {

    private val viewModel by viewModel<SetupBatteryOptimisationViewModel>()

    override fun onAcceptabilityChanged(acceptable: Boolean) {
        if(acceptable) viewModel.moveToNext()
    }

}