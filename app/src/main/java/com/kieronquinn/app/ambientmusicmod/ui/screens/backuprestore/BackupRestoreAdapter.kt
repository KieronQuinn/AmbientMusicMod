package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore

import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView

class BackupRestoreAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items)