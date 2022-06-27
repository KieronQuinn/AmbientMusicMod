## How does Now Playing work?

Now Playing periodically runs a local machine learning model on a 8 second audio recording, 
efficiently finding matches in the track list database. No audio is sent to a server unless you 
specifically request it using the On Demand option.

## Why does Ambient Music Mod require root on some devices?

Ambient Music Mod relies on the "Shell" app (which ADB and Shizuku use) having the 
`CAPTURE_AUDIO_HOTWORD` permission. This appears to have become the standard in Android 12, but
on versions older than that and devices without the permission granted, root is required. This 
cannot be worked around.

## Why is Ambient Music Mod not available on Google Play?

Ambient Music Mod uses a modified version of Android System Intelligence (a Google app), and uses
hidden APIs to achieve these mods. This, in addition to the use of Accessibility, means the app
isn't allowed on Google Play. Instead, it will automatically check for updates on GitHub and notify
you if there are updates available.

## How accurate is Now Playing?

Very accurate. If the song is audible to the device and is in the music database, it will almost
always be recognised correctly. Sometimes it does recognise ambient noise as a song, but this is 
rare.

## I can't get On Device recognition to recognise any music

Firstly, check the track that is playing is actually in the music database. You can do this in the
Updates tab, selecting "View Track List" on the Music Database card. 

If it's in the database, use the "Playback" option in Ambient Music Mod's Recognise feature 
(accessed from the Now Playing screen after recognition has failed), to check the quality of the
recorded audio. It should be audible, but not distorted. If the audio is too quiet, you can either
move closer to the source or try increasing the gain in Settings > Advanced. Note that increasing
the gain will also increase noise, so check after each modification if playback is still audible
without significant distortion.

If you find that the recorded audio is always distorted, even on low gain values, enable 
"Alternative Encoding" in the advanced settings. You may need to increase the gain again once 
enabling this, since it usually makes the recorded audio much quieter.

If the track is not in the database, and you find this is the case with many tracks, consider
switching database country to one that closer matches your music.

## How many songs does Ambient Music Recognise?

It depends on your country. The "core" database (used by all regions) has 16k tracks, but after the 
databases for your country have been downloaded by Now Playing, the list is significantly longer.

At the time of writing, there are around 70k tracks in the United States database.

You can view the full list of tracks Ambient Music is able to recognise on your device using the 
"View Track List" option.

## If Ambient Music Mod can inject code, does it record my conversations and send them somewhere?

Absolutely not. Ambient Music Mod is open source, you can verify it's not doing anything nefarious 
for yourself by checking out the code on [GitHub](https://github.com/KieronQuinn/AmbientMusicMod)

## When does Now Playing skip recognition?

Now Playing will skip recognition (ie. not even attempt to recognise music) during the following 
scenarios:

- When music is being played by the device
- When you are recording audio in another app
- When you are on a call (the microphone is in use by the call regardless, so the call could never
be recorded by Now Playing)
- When Battery Saver is enabled and you have the option to not run when it is enabled in the 
Settings
- When Bedtime Mode is enabled, and it is between the start and end times 

## How much battery does Ambient Music Mod use?

This depends on your settings. You can find a balance that works for you, ranging from running
every minute to only when requested. The following settings may improve battery life for you:

- If you are using periodic recognition, make sure Adaptive Recognition Period is enabled. This
adjusts the next recognition to not occur until the current track is estimated to finish, meaning
the periodic recognitions that would normally occur during this time will be skipped, saving battery

- If you are using periodic recognition, you can reduce how often recognition runs. The default is
every minute, but you can increase it to 3 or 5 minutes if you wish - but this may not recognise
all tracks

- If you do not require periodic recognition, disable it. You can then use the widget, or run
recognition manually from inside Ambient Music Mod when required. Note that this will not recognise
music in the background.

- Enable Bedtime Mode, which disables music recognition between two times when you normally do not
have music playing.

- Try enabling the "Run on Little Cores" option, in the advanced settings. This may not work on your
device, but it may save battery.

## On Demand is not compatible with my device

On Demand - the recognition method that runs online - requires a device that fits certain criteria:

- It must be running Android 12 or above
- It must have an ARM v8 CPU (ARM v7 is not compatible)
- It must have the Google Search App's Music Recognition Service set as the system recognition
service (this is done in the system - no apps can override it)
- It must have the `.29`, bundle build of the Google Search App installed

For more information on setting up your device to use On Demand, 
[see here](https://github.com/KieronQuinn/AmbientMusicMod/wiki/Enabling-On-Demand)

## Can I use Ambient Music Mod on a Pixel?

On devices with official Now Playing support (Pixels from the Pixel 2 series and newer), running
stock firmware, you should continue to use the built-in Now Playing. While Ambient Music Mod aims
to be as efficient as possible, the official Now Playing still uses less battery and takes advantage
of the low-level music model (which can't be ported), meaning it can recognise when music starts
playing, rather than relying on recognition periods like Ambient Music Mod does.

On non-stock firmware on Pixels that do not include the official Now Playing, Ambient Music Mod
will work fine.

## How do I open the file saved from "Playback"?

The `.pcm` file saved from the "Playback" option can be opened in Audacity or similar programs with
the following settings (use File > Import > Raw Data):

- Signed 16 bit PCM
- Big-endian byte order
- 1 channel (mono)
- 16000Hz sample rate

## Can I add my own music to my Stored Tracks?

Unfortunately not, this requires On Demand to recognise and return the "fingerprint" of a track.

If On Demand is available on your device, you may wish to run it on any unrecognised tracks in your
collection - this will recognise them and store them in your Stored Tracks database.

## I have the Lock Screen overlay enabled and the track information shows over my PIN entry

This is normal. Android does not provide a way to know when the PIN entry is showing (for security
reasons), so the overlay cannot be hidden during this time.

## Can I show Now Playing on the Always On (Ambient) Display?

No, it is not possible to display overlays on Ambient Displays.

## Does Ambient Music Mod work with third party Now Playing History apps?

Not currently, although some may update to support it soon. However, many of the previously working 
apps have not been updated to support the newer method used on Pixels that Ambient Music Mod is 
based on, so are likely abandoned and will never be updated.

## How do I uninstall Ambient Music Mod?

Uninstall the Ambient Music Mod app, as well as "Now Playing" from your system settings. If you
are using the Magisk On Demand fix module, uninstall that as well and reboot. If you had the
"Display in Owner Info" option enabled, you may need to reset your Owner Info manually in the system
settings.