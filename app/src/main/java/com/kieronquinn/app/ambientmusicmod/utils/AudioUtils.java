package com.kieronquinn.app.ambientmusicmod.utils;

/**
 *  Helper methods for audio-related manipulation that were proving difficult to convert to Kotlin
 */

public class AudioUtils {

    public static byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    public static short[] applyGain(short[] base, float gain){
        if(base.length > 0){
            for(int i = 0; i < base.length; ++i){
                base[i] = (short)Math.min((int)(base[i] * gain), Short.MAX_VALUE);
            }
        }
        return base;
    }

}
