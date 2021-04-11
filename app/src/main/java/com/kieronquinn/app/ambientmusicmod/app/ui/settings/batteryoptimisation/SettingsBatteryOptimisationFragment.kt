package com.kieronquinn.app.ambientmusicmod.app.ui.settings.batteryoptimisation

import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentBatteryOptimisationBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsBatteryOptimisationFragment: BaseFragment<FragmentBatteryOptimisationBinding>(FragmentBatteryOptimisationBinding::class) {

    private val viewModel by viewModel<SettingsBatteryOptimisationViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenCreated {
            launch {
                viewModel.batteryOptimisationDisabledAMM.collect {
                    setAMMButtonState(it)
                }
            }
            launch {
                viewModel.batteryOptimisationDisabledPAS.collect {
                    setPASButtonState(it)
                }
            }
        }
        with(binding){
            val drawable = batteryOptimisationAvd.drawable as AnimatedVectorDrawable
            drawable.registerAnimationCallback(object: Animatable2.AnimationCallback() {
                override fun onAnimationEnd(d: Drawable?) {
                    drawable.start()
                }
            })
            drawable.start()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getBatteryOptimisationStates()
    }

    private fun setAMMButtonState(optimisationDisabled: Boolean) = with(binding.batteryOptimisationAmm) {
        if(optimisationDisabled){
            setOnClickListener(null)
        }else{
            setOnClickListener { viewModel.onAMMDisableClick() }
        }
        isEnabled = !optimisationDisabled
        icon = if(optimisationDisabled) ContextCompat.getDrawable(context, R.drawable.ic_check) else ContextCompat.getDrawable(context, R.drawable.ic_arrow_right)
    }

    private fun setPASButtonState(optimisationDisabled: Boolean) = with(binding.batteryOptimisationPas) {
        if(optimisationDisabled){
            setOnClickListener(null)
        }else{
            setOnClickListener { viewModel.onPASDisableClick() }
        }
        isEnabled = !optimisationDisabled
        icon = if(optimisationDisabled) ContextCompat.getDrawable(context, R.drawable.ic_check) else ContextCompat.getDrawable(context, R.drawable.ic_arrow_right)
    }

}