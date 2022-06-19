package com.kieronquinn.app.ambientmusicmod;

interface IRecognitionCallback {

     void onRecognitionSucceeded(in RecognitionRequest recognitionRequest, in MediaMetadata result, in Bundle extras);
     void onRecognitionFailed(in RecognitionRequest recognitionRequest, int failureCode);
     void onAudioStreamClosed();

}
