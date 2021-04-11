package com.kieronquinn.app.ambientmusicmod.app.ui.faq

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseFragment
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentFaqBinding
import io.noties.markwon.Markwon

class FaqFragment: BaseFragment<FragmentFaqBinding>(FragmentFaqBinding::class) {

    private val markwon by lazy {
        Markwon.create(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markdownText = requireContext().assets.open("faq.md").bufferedReader().use { it.readText() }
        markwon.setMarkdown(binding.faqText, markdownText)
        (view as NestedScrollView).run {
            scrollBarStyle = ScrollView.SCROLLBARS_OUTSIDE_INSET
            overScrollMode = ScrollView.OVER_SCROLL_NEVER
        }
    }

}