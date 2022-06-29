![Ambient Music Mod Banner](https://i.imgur.com/SPWAuFll.png)

**Ambient Music Mod** | [Now Playing](https://github.com/KieronQuinn/NowPlaying)

Ambient Music Mod is a Shizuku or root app that ports Now Playing from Pixels to other Android devices. 

## Requirements

- Android device running Android 9.0 or above (11+ recommended).
- Shizuku (Android 12+) or root access (Android 9+). 
	- Shizuku does not require root, instead needing an ADB command to be run every reboot.

## Features

- Full Now Playing support, based on the latest version from Pixel devices and the latest music databases
- Automatic Ambient Music recognition, with settings to control how often recognition runs - finding the right balance between battery usage and convenience
- Now Playing History and Favourites support
- Support to trigger recognitions manually, including a homescreen widget
- On Demand recognition on supported devices, using the Google Assistant-backed recognition engine for songs that are not in the local database (**must be triggered manually**)
- Show Now Playing songs on the lock screen (accessibility service required)
- View the full track list of recognisable songs, and change the database location if your taste does not match your device's locale

## Screenshots

[![Screenshots](https://i.imgur.com/RCPP9Sol.png)](https://i.imgur.com/RCPP9So.png)

## Installation

Download the latest APK from the [Releases page](https://github.com/KieronQuinn/AmbientMusicMod/releases) and install it. Ambient Music Mod will download and install the latest Now Playing APK for you as part of the setup process.

If you have any questions, please [read the FAQ](https://github.com/KieronQuinn/AmbientMusicMod/blob/main/app/src/main/res/raw/faq.md) before opening an issue or replying to the XDA thread.

## Building

> Note: If you are building Ambient Music Mod yourself, you **must** also build Now Playing, since the signatures have to match for security reasons.

1. Clone the repository
2. Create a `local.properties` file in the root of the project, and set it up:
```
sdk.dir=<path to your Android SDK>
storeFile=<path to your keystore>
keyAlias=<keystore alias>
storePassword=<keystore password>
keyPassword=<key password>
```
3. Open the project in Android Studio
4. Set the Build Variant to release
5. Run and install a build of `app` as normal

### Building the Magisk Overlay Module

1. Open the project in Android Studio
2. Run the `:ondemandoverlay:buildOverlay` task. A module zip will be built and placed in `ondemandoverlay/build/module`.

## Sources

This repository contains a local version of [google/private-compute-services](https://github.com/google/private-compute-services) ([Apache 2.0 licence](https://github.com/google/private-compute-services/blob/master/LICENSE))

## Read More

Read more, including about how Now Playing works and how it protects your privacy [here](https://medium.com/@KieronQuinn/now-playing-ambient-music-mod-v2-93cd4042cc11)
