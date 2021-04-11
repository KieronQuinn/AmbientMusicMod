## How does Ambient Music work?

Ambient Music detects music playing via "Sound Trigger" (the same service responsible for voice activation of assistants), and then runs a local machine learning model on a 8 second audio recording, using Nearest Neighbour to efficiently find matches in the track list database. By default, this process runs when the device's screen turns on, but Ambient Music Mod allows it to be run periodically to allow recognition while the screen is off or the device is in use.

## Will Ambient Music work on my device?

Check the compatibility in the _Installer_ tab of Ambient Music Mod, to check your device is theoretically compatible. The only way to know for sure if it works is to try it, or for someone else with the same device as you to report whether it works or not. 

## How can I test Ambient Music is working?

Run the Test Recognition, if Ambient Music responds with a successful (or failed) recognition, it is working. Adjust the amplification as described below if required, and you're good to go.

If the Test Recognition fails to respond or gives an error, follow the troubleshooting instructions. If these do not work, the mod may not work on your device. 

## How much battery does Ambient Music use?

Not much, though it's hard to tell. Pixel Ambient Services (which powers Ambient Music) doesn't show up as using _any_ battery while enabled, so measuring the effect is difficult. Throughout building and testing the module, I have not noticed any significant drop in battery life.

## Why does Ambient Music Mod need Xposed, can't it work with just Magisk?

No, Xposed is required. It's important to remember that Magisk does _not_ have the same modification capabilities as Xposed, and while Magisk is great for modifying the `vendor` files and adding the `system` files required for Ambient Music Mod to work, it is not able to inject code at runtime into the system and apps, which Ambient Music Mod requires.

Code is injected to allow bypassing of the normal requirement for Sound Trigger driver version 2.2 or above (required for most devices), and for modifications to make the Ambient Music recognition work on other devices.

## If Ambient Music Mod can inject code, does it record my conversations and send them somewhere?

Absolutely not. Ambient Music Mod is open source, you can verify it's not doing anything nefarious for yourself by checking out the code on [GitHub](https://kieronquinn.co.uk/redirect/ambientmusicmod/github)

## How many songs does Ambient Music recognise?

It depends on your region. The "core" database (used by all regions) has ~17k tracks, but after the databases for your region have been downloaded by Pixel Ambient Services, the list is significantly longer (at the time of writing the GB region has over 71k tracks)

You can view the full list of tracks Ambient Music is able to recognise on your device using the Track List option. If prompted, you may need to update the cache to match the list Ambient Music is using.

## Does Ambient Music listen to my conversations or send audio to a server?

No. The 8 second recording is analysed and immediately discarded (unless you run a manual recognition when it is discarded when you close the response dialog). No audio is sent to a server, and no speech recognition happens, so no conversation data is stored. 

## How accurate is Ambient Music?

Very accurate - it will very rarely get the wrong song. Sometimes it does recognise ambient noise as a song, but this is rare.

## Why does Ambient Music Mod need a specific version of Pixel Ambient Services, can I update it from the Play Store or elsewhere?

For reasons unknown, Google decided to move most of the code related to Ambient Music from Pixel Ambient Services to Device Personalization Services sometime in 2020. Due to the fact that Device Personalization Services is included on many non-Pixel devices (it provides live caption, autofill and a large amount of other non-AOSP services), and the versions between devices differ wildly, it would be impossible to make the Xposed module work for all of them without breaking features on some devices. Therefore, Ambient Music Mod will disable Ambient Music in Device Personalization Services (as some devices have the code inactive, despite not being supported officially) if required, and use the slightly older but still perfectly functional version in Pixel Ambient Services, which is stable.

For this reason, please _**do not update**_ Pixel Ambient Services via the Play Store or other means. If you do, Ambient Music Mod will prompt you to uninstall updates and revert to the version that is compatible.

If Google Play keeps trying to update Pixel Ambient Services, you may find a Magisk module like "Detach" (available on the module repository) helps by detaching the app from the store.

## Does Ambient Music work all the time?

Ambient Music will work almost all the time, so long as you exclude Ambient Music Mod and Pixel Ambient Services from battery optimisation. There are a few exemptions to this, where it is intentionally disabled:

- When you are on a call (the microphone is in use by the call anyway, so no calls could ever be recorded regardless)

- When you are recording audio elsewhere (other recording apps take priority over Ambient Music)

- When you are playing music. There is code in Ambient Music to be able to recognise during music playback, but when enabled it simply 'records' silence, therefore it is disabled.

- When Battery Saver is enabled. Ambient Music is switched off to save battery when this is enabled.

## What amplification level should I use?

This depends on the device - where its microphones are, the built in amplification and their quality. The best way to find your perfect level is to place your device within easily audible distance of a speaker (not too close, not too far), and run test recognitions until you find a level that, when played back, has recognisable music but is not distorted.

## Will Ambient Music work when my device is idle or in my pocket?

Ambient Music will work when idle, as long as battery optimisation is disabled, but may not work when your phone is in your pocket - it depends on the microphone's position and the orientation of your phone.

## Does Ambient Music Mod work with third party Now Playing "History" apps?

Yes! There are numerous Now Playing history apps on the Play Store that are compatible with Ambient Music Mod (as at its core it's the same Pixel Ambient Services app as on Pixels). You may wish to download one of the apps for a more detailed history including location saving of where a track was recognised and scrobbling to last.fm.

## Can I get Ambient Music to display tracks on my lock screen or always on/ambient display?

Ambient Music Mod can display songs on the lock screen using the "Show Now Playing on Lock Screen" option (which uses an Accessibility Service to overlay on top of the lock screen), but this does not work reliably on always on/ambient displays and therefore is set to only display on the lock screen. 

There is one exception to this: If you have a OnePlus device running a recent version of Oxygen OS, the Xposed module [OPAodMod](https://forum.xda-developers.com/t/xposed-16-feb-v3-2-opaodmod-always-on-display-with-lots-of-options.4100981/) is able to display the track on the always on display.

## Can I create a log dump to help fix an issue on my device?

Yes, first enable the developer options (triple tap on the "About" option on the main settings screen), then enable the "Enable Logging" option and reboot. Start Ambient Music when prompted by the notification, run a test recognition (even if it fails). Now use the "Dump Logs" option in the Developer Options to create a zip of logs, and submit that with your bug report.

**Please note that the developer options are only available after installing and enabling the module**

## How do I uninstall Ambient Music Mod?

Uninstall the Magisk module in the Magisk App, uninstall the Ambient Music Mod app and reboot. The mod will be uninstalled and your device be restored to the original sound trigger files.
