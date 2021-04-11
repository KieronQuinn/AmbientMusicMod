package com.kieronquinn.app.ambientmusicmod.components.installer

import android.util.Log
import com.kieronquinn.app.ambientmusicmod.constants.AMBIENT_MUSIC_MODEL_UUID
import com.kieronquinn.app.ambientmusicmod.constants.MODEL_UUID
import com.kieronquinn.app.ambientmusicmod.constants.MODULE_VERSION_CODE_PROP
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SystemProperties_getInt
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer


class SoundTriggerPlatformXML(private val xmlSerializer: XmlSerializer) {

    private val isAlreadyInstalled
        get() = SystemProperties_getInt(MODULE_VERSION_CODE_PROP, 0) > 0

    /**
     *  Clones the current tag XmlPullParser is on to the serializer
     */
    private fun XmlPullParser.cloneTag(){
        when(eventType){
            XmlPullParser.START_DOCUMENT -> {
                xmlSerializer.startDocument(Charsets.ISO_8859_1.name(), null)
            }
            XmlPullParser.DOCDECL -> {
                xmlSerializer.docdecl(text)
            }
            XmlPullParser.START_TAG -> {
                xmlSerializer.startTag("", name)
                for(i in 0 until attributeCount){
                    xmlSerializer.attribute("", getAttributeName(i), getAttributeValue(i))
                }
            }
            XmlPullParser.TEXT -> {
                xmlSerializer.text(text)
            }
            XmlPullParser.END_TAG -> {
                xmlSerializer.endTag("", name)
            }
            XmlPullParser.COMMENT -> {
                xmlSerializer.comment(text)
            }
            XmlPullParser.CDSECT -> {
                xmlSerializer.cdsect(text)
            }
            XmlPullParser.ENTITY_REF -> {
                xmlSerializer.entityRef(text)
            }
            XmlPullParser.IGNORABLE_WHITESPACE -> {
                xmlSerializer.ignorableWhitespace(text)
            }
            XmlPullParser.PROCESSING_INSTRUCTION -> {
                xmlSerializer.processingInstruction(text)
            }
        }
    }

    /**
     *  Iterates through the XML, cloning as it goes, until it finds a suitable point for injecting the required config/usecase
     */
    fun XmlPullParser.skipUntilInjectionPoint(): CurrentXMLType {
        if(isAlreadyInstalled) return CurrentXMLType.ALREADY_INSTALLED
        while(eventType != XmlPullParser.END_DOCUMENT){
            when(eventType){
                XmlPullParser.START_TAG -> {
                    if(name == "param"){
                        val vendorUuid = getAttributeValue("", "vendor_uuid")
                        if(vendorUuid != null && vendorUuid == MODEL_UUID) {
                            //We've found an existing Google Music Detector sound config
                            return CurrentXMLType.PARTIAL_MUSIC
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if(name == "sound_trigger_platform_info"){
                        //Reached the end of the XML without finding the Google Music usecase, append to the end
                        return CurrentXMLType.NO_MUSIC
                    }
                }
            }
            cloneTag()
            nextToken()
        }
        //Reached end of XML without finding a suitable point for injecting, this XML is not compatible
        return CurrentXMLType.INCOMPATIBLE
    }

    /**
     *  Iterates until the next end of sound_model_config for overwrite
     */
    fun XmlPullParser.skipUntilEndOfSoundModelConfig(){
        while(name != "sound_model_config"){
            next()
        }
    }

    /**
     *  Skips current tag (ignoring children)
     */
    private fun XmlPullParser.skip() {
        check(eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    /**
     *  Helper function to run cloneTag() on all remaining tags until the end of the document
     */
    fun XmlPullParser.cloneUntilEndOfDocument(){
        while(eventType != XmlPullParser.END_DOCUMENT){
            cloneTag()
            nextToken()
        }
    }

    private var tabDepth = 0

    /**
     *  Writes the working Google Music Detection section in full to the XML. This may not work on all devices, ideally it should already have a partial setup
     */
    fun writeGoogleMusicDetectionSection(initialTabDepth: Int = 0) = with(xmlSerializer) {
        tabDepth = initialTabDepth
        comment("Google Music Detection", true)
        tag("sound_model_config"){
            writeParitalMusicSection()
        }
    }

    /**
     *  Writes just section after the UUID param
     */
    fun writeParitalMusicSection(initialTabDepth: Int? = null) = with(xmlSerializer) {
        initialTabDepth?.let {
            tabDepth = it
        }
        param("vendor_uuid", "9f6ad62a-1f0b-11e7-87c5-40a8f03d3f15")
        param("execution_type", "ADSP")
        param("library", "none")
        param("max_ape_phrases", "1")
        param("max_ape_users", "1")
        comment("Profile specific data which the algorithm can support", true)
        param("sample_rate", "16000")
        param("bit_width", "16")
        param("out_channels", "1", false)
        comment("Module output channels", false)
        comment("adm_cfg_profile should match with the one defined under adm_config", true)
        comment("Set it to NONE if LSM directly connects to AFE", true)
        param("adm_cfg_profile", "FLUENCE")
        comment("fluence_type: \"FLUENCE\", \"FLUENCE_DMIC\"", true)
        comment("\"FLUENCE_QMIC\". param value is valid when adm_cfg_profile=\"FLUENCE\"", true)
        param("fluence_type", "FLUENCE_DMIC")
        comment("fluence enabled: \"FLUENCE_DMIC\", \"FLUENCE_QMIC\"", true)
        param("wdsp_fluence_type", "NONE")
        param("app_type", "4", false)
        comment("app type used in ACDB", false)
        param("in_channels", "2", false)
        comment("Module input channels", false)
        comment("format: \"ADPCM_packet\" or \"PCM_packet\"", true)
        comment("transfer_mode: \"FTRT\" or \"RT\"", true)
        comment("kw_duration is in milli seconds. It is valid only for FTRT transfer mode", true)
        param("capture_keyword", "PCM_raw, FTRT, 5000")
        param("client_capture_read_delay", "2000")
        tag("gcs_usecase"){
            param("uid", "0x5")
            param("acdb_devices", "DEVICE_HANDSET_MIC_CPE")
            param("load_sound_model_ids", "0x18000001, 0x4, 0x18000102")
            param("start_engine_ids", "0x18000001, 0x4, 0x18000103")
            param("request_detection_ids", "0x18000001, 0x4, 0x18000107")
            param("confidence_levels_ids", "0x18000001, 0x4, 0x00012C28")
            param("detection_event_ids", "0x18000001, 0x4, 0x00012C29")
            param("custom_config_ids", "0x18000001, 0x4, 0x00012C20")
            param("read_cmd_ids", "0x00020013, 0x6, 0x00020015")
            param("read_rsp_ids", "0x00020013, 0x6, 0x00020016")
        }
        tag("lsm_usecase"){
            param("execution_mode", "ADSP")
            param("load_sound_model_ids", "0x18100000, 0x0, 0x00012C14")
            param("unload_sound_model_ids", "0x18100000, 0x0, 0x00012C15")
            param("confidence_levels_ids", "0x18100000, 0x0, 0x00012C07")
            param("operation_mode_ids", "0x18100000, 0x0, 0x00012C02")
            param("polling_enable_ids", "0x18100000, 0x0, 0x00012C1B")
            param("custom_config_ids", "0x18100000, 0x0, 0x00012C20")
        }
    }

    /**
     *  Helper function to create a tag with the name "param", and a given key value pair
     */
    private fun param(key: String, value: String, newline: Boolean = true){
        tag("param", Pair(key, value), newline = newline)
    }

    /**
     *  Creates an XML tag in full, with optional children
     */
    private fun tag(tagName: String, newline: Boolean = true, children: (() -> Unit)? = null){
        tabs()
        xmlSerializer.startTag("", tagName)
        xmlSerializer.ignorableWhitespace("\n")
        tabDepth++
        children?.invoke()
        tabDepth--
        tabs()
        xmlSerializer.endTag("", tagName)
        if(newline) xmlSerializer.ignorableWhitespace("\n")
    }

    /**
     *  Creates an XML tag in full with no children and a given set of key value pair attributes
     */
    private fun tag(tagName: String, vararg attributes: Pair<String, String>, newline: Boolean = true){
        tabs()
        xmlSerializer.startTag("", tagName)
        attributes.forEach {
            xmlSerializer.attribute("", it.first, it.second)
        }
        xmlSerializer.endTag("", tagName)
        if(newline) xmlSerializer.ignorableWhitespace("\n")
    }

    /**
     *  Creates an XML comment with a newline after it
     */
    private fun comment(text: String, tabs: Boolean){
        if(tabs) tabs()
        else xmlSerializer.ignorableWhitespace(" ")
        xmlSerializer.comment(" $text ")
        xmlSerializer.ignorableWhitespace("\n")
    }

    /**
     *  Appends tabs to the XML for formatting (the original sound_trigger_platform.xml files have formatting, much like most Android system XMLs)
     */
    private fun tabs(){
        for(i in 0 until tabDepth){
            xmlSerializer.ignorableWhitespace("\t")
        }
    }

    enum class CurrentXMLType {
        ALREADY_INSTALLED,
        NO_MUSIC,
        PARTIAL_MUSIC,
        INCOMPATIBLE
    }

    private val XmlPullParser.isUsecaseTag
        get() = name == "gcs_usecase" || name == "lsm_usecase"

}