package com.kieronquinn.app.ambientmusicmod.constants

import com.kieronquinn.app.ambientmusicmod.BuildConfig
import java.util.*

const val BUILD_MODULE_VERSION = BuildConfig.BUILD_MODULE_VERSION
const val BUILD_MODULE_VERSION_CODE = BuildConfig.BUILD_MODULE_VERSION_CODE
const val SOUND_TRIGGER_PLATFORM_PATH = "/system/vendor/etc/sound_trigger_platform_info.xml"
const val MODULE_VERSION_PROP = "ro.ambientmusicmod.version"
const val MODULE_VERSION_CODE_PROP = "ro.ambientmusicmod.version_code"
const val MODEL_UUID = "9f6ad62a-1f0b-11e7-87c5-40a8f03d3f15"

//Required Pixel Ambient Services version for Xposed module to work
const val PIXEL_AMBIENT_SERVICES_VERSION = 1183L

//Redirect URLs just in case threads / usernames change
const val XDA_THREAD_REDIRECT_URL = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/xda"
const val DONATE_REDIRECT_URL = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/donate"
const val TWITTER_REDIRECT_URL = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/twitter"
//Will also need changing in faq.md
const val GITHUB_REDIRECT_URL = "https://kieronquinn.co.uk/redirect/AmbientMusicMod/github"

val AMBIENT_MUSIC_MODEL_UUID: UUID = UUID.fromString(MODEL_UUID)

/**
 *  The minimum version of android.hardware.soundtrigger that is supported. 2.1 support is bodged
 *  via Xposed, 2.2+ *may* work without Xposed intervention but Xposed is still needed for other mods
 */
const val MIN_SOUND_TRIGGER_VERSION = 2.1

//These also need to be declared in the manifest for Android 11+ support
val xposedApps = arrayOf(
        "org.meowcat.edxposed.manager",
        "org.lsposed.manager"
)