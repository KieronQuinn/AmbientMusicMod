package com.kieronquinn.app.ambientmusicmod.ui.screens.faq

import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentFaqBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getColorResCompat
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import org.commonmark.node.Heading

class FaqFragment : BoundFragment<FragmentFaqBinding>(FragmentFaqBinding::inflate), BackAvailable {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
        val monoTypeface = ResourcesCompat.getFont(requireContext(), R.font.google_sans_mono)
        val markwon = Markwon.builder(requireContext()).usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                typeface?.let {
                    builder.headingTypeface(it)
                    builder.headingBreakHeight(0)
                }
                monoTypeface?.let {
                    builder.codeBlockTypeface(it)
                }
            }

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                val origin = builder.requireFactory(Heading::class.java)
                builder.setFactory(Heading::class.java) { configuration, props ->
                    arrayOf(
                        origin.getSpans(configuration, props),
                        ForegroundColorSpan(
                            requireContext().getColorResCompat(android.R.attr.textColorPrimary)
                        )
                    )
                }
            }
        }).usePlugin(CorePlugin.create()).build()
        val markdown = requireContext().resources.openRawResource(R.raw.faq).bufferedReader()
            .use { it.readText() }
        markwon.setMarkdown(binding.markdown, markdown)
        binding.markdown.setLinkTextColor(monet.getAccentColor(requireContext()))
        binding.root.applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

}