package com.kieronquinn.app.ambientmusicmod

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.kieronquinn.app.ambientmusicmod.components.LevelDBParser
import junit.framework.Assert.assertTrue
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 *  Tests the LevelDB parsing component, downloading *all* the databases from the Google set, finding the end offsets and parsing them
 *  If you would like to see an output of tracks, specify an output dir with "test.output.csv_dir=<path>" in your local.properties file and run the test, a CSV file will be made for each one.
 */
class LevelDBParsingTest {

    companion object {
        //Manifest JSON url: This changes with the database updates and unfortunately comes from Phenotypes so there's no easy way to make it auto-update
        private val JSON_URL = "https://storage.googleapis.com/music-iq-db/updatable_ytm_db/20210411-030029/manifest.json"
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `test LevelDB parser on regions from Google set`(){
        val request = Request.Builder().apply {
            url(JSON_URL)
        }.build()
        val responseBody = okHttpClient.newCall(request).execute().body()!!.string()
        val jsonResponse = JSONObject(responseBody)
        val packs = jsonResponse.getJSONArray("packs")
        val packList = ArrayList<Pack>()
        for(i in 0 until packs.length()){
            val packItem = packs.getJSONObject(i)
            val countryCode = if(packItem.has("extra_country_codes")){
                packItem.getString("extra_country_codes")
            }else "core"
            val url = packItem.getJSONArray("download_urls").getString(0)
            val name = packItem.getString("name")
            val type = packItem.getString("extra_type")
            packList.add(Pack(countryCode, url, name, type))
        }
        packList.sortBy { it.countryCode }
        println(packList.joinToString(", ") { it.name })
        println(folder.root.absolutePath)
        val outputCsvFolder = if(BuildConfig.TEST_OUTPUT_CSV_DIR.isNotEmpty()){
            File(BuildConfig.TEST_OUTPUT_CSV_DIR)
        }else null
        outputCsvFolder?.mkdirs()
        packList.forEachIndexed { index, pack ->
            val outputCsvFile = if(outputCsvFolder == null) null else
                    File(outputCsvFolder, "${pack.name}.csv")
            if(outputCsvFile?.exists() == true) return@forEachIndexed
            val downloadStartTime = now
            println("Downloading ${pack.name} (${pack.url}) ($index/${packList.size})")
            val outputFile = folder.newFile(pack.name)
            downloadToFile(pack.url, outputFile)
            println("Download of ${pack.name} completed in ${now - downloadStartTime}ms")
            println("Finding end offset for ${pack.name}")
            val offsetStartTime = now
            val offset = LevelDBParser.findMatcherFileEndOffset(outputFile)
            assertTrue("Offset not found in ${pack.name}", offset != null)
            println("Found offset $offset in ${now - offsetStartTime}ms")
            println("Parsing ${pack.name} (endOffset $offset) ($index/${packList.size})")
            val parseStartTime = now
            val tracks = LevelDBParser.parseMatcherFile(outputFile, offset!!)
            println("Found ${tracks.size} tracks in ${pack.name} in ${now - parseStartTime}ms")
            if(outputCsvFile != null){
                csvWriter().open(outputCsvFile) {
                    tracks.forEach {
                        writeRow(listOf(it.artist, it.track))
                    }
                }
                println("Written to CSV ${outputCsvFile.name}")
            }
            assertTrue("Didn't find any tracks in ${pack.name}", tracks.isNotEmpty())
        }
    }

    private val now
        get() = System.currentTimeMillis()

    private fun downloadToFile(url: String, outputFile: File){
        val request = Request.Builder().apply {
            url(url)
        }.build()
        okHttpClient.newCall(request).execute().body()!!.byteStream().buffered().copyTo(outputFile.outputStream().buffered())
    }

    data class Pack(val countryCode: String, val url: String, var name: String, val type: String)

}