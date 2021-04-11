package com.kieronquinn.app.ambientmusicmod.components

import android.util.Log
import com.kieronquinn.app.ambientmusicmod.model.database.Track
import java.io.File
import java.io.RandomAccessFile
import java.lang.Exception
import java.math.BigInteger

object LevelDBParser {

    private const val TYPE_STRING_TRACK: Byte = 0x1A
    private const val TYPE_STRING_ARTIST: Byte = 0x22
    private const val TYPE_STRING_TRACK_ARTIST_END: Byte = 0x31

    //Bit of a hack: The max length before a track/artist is immediately rejected with no further checking. Fixes an edge case.
    private const val MAX_ARTIST_LENGTH = 150
    private const val MAX_TRACK_LENGTH = 100

    private val BYTES_END_OF_USEFUL_DATA = BigInteger("52000000", 16).toByteArray()

    /**
     *  This is essentially a brute force parse of a matcher file - either a core (matcher_tah / tree_core_shard), or one of the metadata files -
     *  which is able to extract a list of Artist / Track pairs
     *  It uses the following logic:
     *  Track & artist pairs are contained within the file, each followed by some raw data (might be related to the recognition)
     *  As I was unable to figure out how to parse this data, I chose to simply create a searching method that looked for track & artist pairs.
     *  Each pair is as follows:
     *  [Track Header (0x1A)] [Integer "tl" (track length)] [Track (length=tl)] [Artist Header (0x22)] [Integer "al" (artist length)] [Artist (length=al)] [Header of data (0x31)] [...]
     *  Given the header 0x1A appears at offset n, we can check that n + (1 + trackLength) = artist header (0x22), and that
     *  n + (1 + trackLength) + (1 + artistLength) = data header (0x31), and thus can pretty reliably know we've found a track and artist pair.
     *  Combined with a search for the "end of useful data" (0x52 0x00 0x00 0x00), and ending the search at the offset of those bytes,
     *  parsing of the actual file is relatively quick (matcher_tah in 6.1 seconds), but as finding the end of useful data offset is much slower
     *  (up to 30 seconds on the same device) for matcher_tah, ideally we don't want to do that every time. Therefore, the end of useful data offset
     *  for matcher_tah is hardcoded into the app, and is only located for metadata files (which are about 1/5 the size and have a much smaller offset)
     */
    fun parseMatcherFile(file: File, endOfData: Long): List<Track> {
        val tracks = ArrayList<Track>()
        var startTime = System.currentTimeMillis()
        RandomAccessFile(file, "r").use {
            var offset = 0L
            it.seek(offset)
            startTime = System.currentTimeMillis()
            while (offset < endOfData) {
                val lastOffset = offset
                if (it.readByte() == TYPE_STRING_TRACK) {
                    val trackLength = it.readByteIntoInt()
                    if (trackLength in 1 until MAX_TRACK_LENGTH) {
                        it.seek(it.filePointer + trackLength)
                        val artistHeader = it.readByte()
                        if (artistHeader == TYPE_STRING_ARTIST) {
                            val artistLength = it.readByteIntoInt()
                            if (artistLength in 1 until MAX_ARTIST_LENGTH) {
                                it.seek(it.filePointer + artistLength)
                                val endArtistHeader = it.readByte()
                                if (endArtistHeader == TYPE_STRING_TRACK_ARTIST_END) {
                                    //Found a track, return to start and parse
                                    it.seek(lastOffset)
                                    //Skip track header & length
                                    it.skipBytes(2)
                                    //Read the track
                                    val track = ByteArray(trackLength).apply {
                                        it.readFully(this)
                                    }
                                    //Skip the artist header & length
                                    it.skipBytes(2)
                                    //Read the album
                                    val artist = ByteArray(artistLength).apply {
                                        it.readFully(this)
                                    }
                                    //Log.d("LevelDB", "Found ${String(track)} by ${String(artist)}")
                                    tracks.add(Track(String(track), String(artist)))
                                    //Skip the footer
                                    it.readByte()
                                } else {
                                    it.seek(lastOffset)
                                    it.skipBytes(1)
                                }
                            } else {
                                it.seek(lastOffset)
                                it.skipBytes(1)
                            }
                        } else {
                            it.seek(lastOffset)
                            it.skipBytes(1)
                        }
                    }
                }
                offset = it.filePointer
            }
        }
        Log.d("LevelDB", "Found ${tracks.size} tracks in file ${file.name} in ${System.currentTimeMillis() - startTime}ms")
        return tracks
    }

    fun findMatcherFileEndOffset(file: File): Long? {
        return try {
            RandomAccessFile(file, "r").findByteArray(BYTES_END_OF_USEFUL_DATA)
        }catch (e: Exception){
            null
        }
    }

    private fun RandomAccessFile.readByteIntoInt(): Int {
        return readByte().toInt()
    }

    /**
     *  Searches a file for a given ByteArray, returning its offset. This is very slow.
     *  It iterates through the file one byte at a time, checking if the following 6 bytes are equal to the required bytes
     *  TODO see if this can be sped up using the same method as grep
     */
    private fun RandomAccessFile.findByteArray(bytes: ByteArray): Long {
        var offset = 0L
        while (offset < length() - bytes.size) {
            seek(offset)
            //Move forward by 1
            skipBytes(1)
            //Update offset for next loop
            val nextOffset = filePointer
            //Get next x bytes
            val nextX = ByteArray(bytes.size).apply {
                readFully(this)
            }
            //Check if they equal
            if (nextX.contentEquals(bytes)) return filePointer
            offset = nextOffset
        }
        //Not in file
        return length()
    }

}