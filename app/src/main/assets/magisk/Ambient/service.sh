#!/system/bin/sh

# Wait for package service to start
log -p v -t "Ambient" "Ambient Start"

while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done;

log -p v -t "Ambient" "Boot complete"

# Disable Ambient Music in com.google.android.as (Device Personalisation Services) if it exists, as we want to use Pixel Ambient services, and it interferes with that

pm disable "com.google.android.as/com.google.intelligence.sense.common.SystemBroadcastReceiver"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.history.HistoryContentProvider"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.history.AddShortcutActivity"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.getmodelstate.ScheduledGetModelStateService"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.history.HistoryActivity"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.history.HistoryGarbageCollector"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.reload.ModelReloadService"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.InternalBroadcastReceiver"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.AmbientMusicDetector\$Receiver"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.AmbientMusicDetector\$Service"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.AmbientMusicSetupWizardActivity"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.AmbientMusicSettingsActivity"
pm disable "com.google.android.as/com.google.intelligence.sense.ambientmusic.AmbientMusicNotificationsSettingsActivity"
pm disable "com.google.android.as/com.google.android.apps.miphone.aiai.nowplaying.api.NowPlayingService"
 
log -p v -t "Ambient" "Disable complete"

# Set Ambient Music Mod version in the build.prop

