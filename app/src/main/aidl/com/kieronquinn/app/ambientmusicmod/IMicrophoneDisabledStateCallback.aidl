package com.kieronquinn.app.ambientmusicmod;

interface IMicrophoneDisabledStateCallback {
    oneway void onMicrophoneDisabledStateChanged(boolean disabled);
}