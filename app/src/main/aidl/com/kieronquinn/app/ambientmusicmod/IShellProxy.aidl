package com.kieronquinn.app.ambientmusicmod;

import com.kieronquinn.app.ambientmusicmod.IRecognitionCallback;
import com.kieronquinn.app.ambientmusicmod.IMicrophoneDisabledStateCallback;

interface IShellProxy {

    boolean ping() = 1;
    boolean isCompatible() = 2;
    boolean isRoot() = 3;

    //AudioRecord proxy
    void AudioRecord_create(in AudioAttributes attributes, in AudioFormat audioFormat, int sessionId, int bufferSizeInBytes) = 4;
    void AudioRecord_startRecording() = 5;
    void AudioRecord_release() = 6;
    int AudioRecord_read(out byte[] audioData, int offsetInShorts, int sizeInShorts) = 7;
    AudioFormat AudioRecord_getFormat() = 8;
    int AudioRecord_getBufferSizeInFrames() = 9;
    int AudioRecord_getSampleRate() = 10;

    //MusicRecognitionManager proxy (only used externally)
    void MusicRecognitionManager_beginStreamingSearch(in RecognitionRequest request, in IRecognitionCallback callback) = 11;

    //Sensor Privacy checks for AMM UI & to know when to not recognise
    boolean isMicrophoneDisabled() = 12;
    String addMicrophoneDisabledListener(in IMicrophoneDisabledStateCallback callback) = 13;
    void removeMicrophoneDisabledListener(String id) = 14;

    //Get SystemUI package name without requiring QUERY_ALL_PACKAGES
    String getSystemUIPackageName() = 15;

    //Grant the required permission to enable the accessibility service (Android 13+)
    void grantAccessibilityPermission() = 16;

    //Dismiss Keyguard without an Activity
    oneway void dismissKeyguard(in IBinder callback, String message) = 17;

    //Sets the Lock Screen owner info to a given string. Requires root, AMM only.
    oneway void setOwnerInfo(String info) = 18;

    //Force stops Now Playing to force a reload of data
    oneway void forceStopNowPlaying() = 19;

    //MusicRecognitionManager proxy with added thread injection (not exposed externally)
    void MusicRecognitionManager_beginStreamingSearchWithThread(
        in RecognitionRequest request,
        in IRecognitionCallback callback,
        in IBinder thread,
        in IBinder token
    ) = 20;

    void destroy() = 16777114;

}