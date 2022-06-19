package com.kieronquinn.app.ambientmusicmod.ui.screens.recognition

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.ItemRecognitionSuccessPlayerChipBinding
import com.kieronquinn.app.ambientmusicmod.model.recognition.Player
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import kotlinx.coroutines.flow.collect

class RecognitionChipsAdapter(
    var items: List<Player>,
    private val onChipClicked: (Intent) -> Unit,
    recyclerView: LifecycleAwareRecyclerView
): LifecycleAwareRecyclerView.Adapter<RecognitionChipsAdapter.ViewHolder>(recyclerView) {

    private val layoutInflater = LayoutInflater.from(recyclerView.context)

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRecognitionSuccessPlayerChipBinding.inflate(
            layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.binding.recognitionSuccessPlayerChip) {
            val item = items[position]
            setChipBackgroundColorResource(item.chipColour)
            setTextColor(ContextCompat.getColor(context, item.chipTextColour))
            setChipIconResource(item.icon)
            setText(item.name)
            setChipIconTintResource(item.chipTextColour)
            holder.lifecycleScope.launchWhenResumed {
                onClicked().collect {
                    onChipClicked(item.getIntent())
                }
            }
        }
    }

    data class ViewHolder(val binding: ItemRecognitionSuccessPlayerChipBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}