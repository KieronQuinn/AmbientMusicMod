package com.kieronquinn.app.ambientmusicmod.components.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.utils.autoCleared
import kotlin.reflect.KClass

abstract class BaseFragment<T: ViewBinding>(private val viewBindingClass: KClass<T>): Fragment() {

    internal var binding by autoCleared<T>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = viewBindingClass.java.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java).invoke(null, inflater, container, false) as T
        return binding.root
    }

}