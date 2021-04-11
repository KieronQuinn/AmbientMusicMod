package com.kieronquinn.app.ambientmusicmod.xposed.wrappers

import android.media.AudioFormat

/**
 *  Wraps the @hide class android.hardware.soundtrigger.SoundTrigger and its subclasses to allow for nicer calls in the Xposed classes
 */
class SoundTrigger(original: Any, classLoader: ClassLoader): BaseWrapper(original, classLoader) {

    companion object {
        const val RECOGNITION_STATUS_GET_STATE_RESPONSE = 3
    }

    override val originalClass: Class<*> = Class.forName("android.hardware.soundtrigger.SoundTrigger", false, classLoader)

    class StatusListener(original: Any, classLoader: ClassLoader) : BaseWrapper(
        original,
        classLoader
    ) {

        companion object {
            fun getClass(classLoader: ClassLoader): Class<*> {
                return Class.forName("android.hardware.soundtrigger.SoundTrigger\$StatusListener", false, classLoader)
            }
        }

        override val originalClass: Class<*> = getClass(classLoader)

        fun onRecognition(recognitionEvent: GenericRecognitionEvent){
            val recognitionEventClass = Class.forName("android.hardware.soundtrigger.SoundTrigger\$RecognitionEvent", false, classLoader)
            originalClass.getMethod("onRecognition", recognitionEventClass).invoke(original, recognitionEvent.original)
        }

    }

    class GenericRecognitionEvent(
        override val original: Any,
        override val classLoader: ClassLoader,
        override val originalClass: Class<*>
    ) : BaseWrapper(original, classLoader) {

        constructor(
            classLoader: ClassLoader,
            status: Int,
            soundModelHandle: Int,
            captureAvailable: Boolean,
            captureSession: Int,
            captureDelayMs: Int,
            capturePreambles: Int,
            triggerInData: Boolean,
            captureFormat: AudioFormat,
            data: ByteArray
        ) : this(
            createGenericRecognitionEvent(
                getClass(classLoader),
                status,
                soundModelHandle,
                captureAvailable,
                captureSession,
                captureDelayMs,
                capturePreambles,
                triggerInData,
                captureFormat,
                data
            ), classLoader, getClass(classLoader)
        )


        companion object {

            fun getClass(classLoader: ClassLoader): Class<*> {
                return Class.forName(
                    "android.hardware.soundtrigger.SoundTrigger\$GenericRecognitionEvent",
                    false,
                    classLoader
                )
            }

            fun createGenericRecognitionEvent(
                clazz: Class<*>,
                status: Int,
                soundModelHandle: Int,
                captureAvailable: Boolean,
                captureSession: Int,
                captureDelayMs: Int,
                capturePreambles: Int,
                triggerInData: Boolean,
                captureFormat: AudioFormat,
                data: ByteArray
            ): Any {
                return clazz.getConstructor(
                    Integer.TYPE,
                    Integer.TYPE,
                    Boolean::class.java,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Boolean::class.java,
                    AudioFormat::class.java,
                    ByteArray::class.java
                )
                    .newInstance(
                        status,
                        soundModelHandle,
                        captureAvailable,
                        captureSession,
                        captureDelayMs,
                        capturePreambles,
                        triggerInData,
                        captureFormat,
                        data
                    )
            }
        }

    }

}