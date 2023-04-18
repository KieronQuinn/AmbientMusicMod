# Astrea

This module is a stripped down version of [Private Compute Services](https://github.com/google/private-compute-services),
Google's open source component of Android System Intelligence ([Available on Google Play](https://play.google.com/store/apps/details?id=com.google.android.as.oss)).
It provides the core network functionality of Now Playing, downloading the databases (album art does not use Astrea). 
This way, Now Playing does not require the `INTERNET` permission, and all network traffic is well-defined and transparent, 
even when Android System Intellgence itself is closed source. 

Due to it not being a direct fork, updates must be performed manually, following these steps:

- Pull the latest [private-compute-services](https://github.com/google/private-compute-services) code
- Update all the Java classes in `src/main/java`, copying newer versions on top
  - Some classes from the repo have packages which do not match their location. This angers Android Studio. For these, simply fix their imports in classes which use them.
- Add in any additional classes which are required for the existing Java classes
- Update the .proto files in `src/main/proto`. Please note that the proto files are in different locations in this module than the repo, you may need to search for them.
- Open `PcsHttpConfigReader` and set the default value for `PcsHttp__write_to_pfd` to `true` rather than it depending on the Android version.

Compile the app. You may also need to update dependencies, depending on how long it has been since the last time.

Private Compute Services is licenced under the [Apache Licence 2.0](https://github.com/google/private-compute-services/blob/master/LICENSE)