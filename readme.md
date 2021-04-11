# Ambient Music Mod

A hybrid Xposed & Magisk module that ports Pixel Ambient Music to other, compatible devices. If you don't know what Pixel Ambient Music is, it's the feature that recognises music that's playing in the background (ie. not from your phone) automatically. It does this locally, without a connection, and doesn't send any data to Google servers for recognition.

The aim of this feature is simple - you'll never wonder "what's that song" again when around the house or out and about - as your phone will tell you without you even having to ask.

Please [read the FAQ](https://github.com/KieronQuinn/AmbientMusicMod/blob/main/app/src/main/assets/faq.md) before asking questions or reporting issues

## Features

- Full Ambient Music support, including downloading the latest databases from Google

- Music recognition when the device is idle or in use (exclusions apply, see the FAQ for more details)

- Now Playing History built in, and support for third party history apps

- The ability to display the current now playing track on the lock screen using an Accessibility overlay service

- Manual recognition on demand in the Ambient Music Mod app

- Settings to control the amplification, how often recognition should be triggered, whether to run on the small CPU cores and what to do when a song recognition notification is tapped

- View all the recognisable track in your locally downloaded database via the Track List option

## Requirements

- Magisk

- Xposed

- A device with a Snapdragon processor that supports Sound Trigger 2.1 or above (the app will tell you if it's compatible)

## Installation Instructions

- Install the latest release APK from the [releases page](https://github.com/KieronQuinn/AmbientMusicMod/releases)

- Open the app, check your device is compatible and build the module using the Build Installer option

- Install the built Magisk module using the Magisk app

- Enable the Xposed module in Xposed Manager

- Reboot

## Screenshots

Ambient Music Mod displaying the currently playing track on the lock screen of a OnePlus 7T Pro running Oxygen OS 11:

![Ambient Music Mod on lock screen](https://i.imgur.com/vBvVYUDl.png)

Settings, Installer, standard Ambient Music settings & Now Playing history 

![Ambient Music Mod](https://i.imgur.com/8IRTEUL.png)

You can verify that the songs displayed in these screenshots were being played at the time by viewing an screenshot of the playlist from the radio station being listened to at the time [here](https://i.imgur.com/Qhpqnsf.png)
