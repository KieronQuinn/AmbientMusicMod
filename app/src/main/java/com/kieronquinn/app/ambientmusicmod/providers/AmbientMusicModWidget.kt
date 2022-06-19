package com.kieronquinn.app.ambientmusicmod.providers

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.ambientmusicmod.repositories.WidgetRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class AmbientMusicModWidget: AppWidgetProvider(), KoinComponent {

    private val widgetRepository by inject<WidgetRepository>()

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        widgetRepository.notifyChanged()
    }

}