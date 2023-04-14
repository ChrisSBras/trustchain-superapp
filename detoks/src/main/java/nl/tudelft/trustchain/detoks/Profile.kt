package nl.tudelft.trustchain.detoks

import com.frostwire.jlibtorrent.TorrentInfo
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper
import kotlin.math.min
import kotlin.math.max


class ProfileEntry(
    var watched:    Boolean = false,    // True if the video was watched
    var lastUpdate: Long = 0,           // Updated when torrent received
    var watchTime:  Long = 0,           // Average watch time
    var duration:   Long = 0,           // Video duration
    var uploadDate: Long = 0,           // This is the torrent creation date
    var hopCount:   Int  = 0,           // Amount of other nodes visited
    var timesSeen:  Int  = 0,           // Count of times we received it
    var likes:      Int  = 0,           // TODO: Dependent on other team
) {

}

class Profile(
    val profiles: HashMap<String, ProfileEntry> = HashMap()
) {
    object ProfileConfig { const val MAX_DURATION_FACTOR  = 10 }

    fun addProfile(key: String) {
        if(!profiles.contains(key)) incrementTimesSeen(key) // Also creates the profile
        profiles[key]!!.lastUpdate = System.currentTimeMillis()
    }

    fun hasWatched(key: String): Boolean {
        return profiles.contains(key) && profiles[key]!!.watched
    }

    fun mergeWith(key: String, otherEntry: ProfileEntry) {
        val entry = if(profiles.containsKey(key)) profiles[key] else ProfileEntry(timesSeen = 0)
        entry!!.duration = max(entry.duration, otherEntry.duration)     // Pick the non-zero value
        entry.uploadDate = max(entry.uploadDate, otherEntry.uploadDate) // Pick the non-zero value

        profiles[key] = entry
        updateEntryWatchTime(key, otherEntry.watchTime, false)
        updateEntryLikes(key, otherEntry.likes, false)
    }

    fun updateEntryDuration(key: String, duration: Long) {
        addProfile(key)
        profiles[key]!!.duration = duration
    }

    fun updateEntryHopCount(key: String, hopCount: Int) {
        addProfile(key)
        profiles[key]!!.hopCount = hopCount
    }

    fun updateEntryLikes(key: String, likes: Int, myUpdate: Boolean) {
        addProfile(key)
        if(myUpdate) {
            profiles[key]!!.watchTime += (likes / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            profiles[key]!!.watchTime += likes
            profiles[key]!!.watchTime /= 2
        }
    }

    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        addProfile(key)
        if(myUpdate) {
            val newTime = min(time, profiles[key]!!.duration  * ProfileConfig.MAX_DURATION_FACTOR)
            profiles[key]!!.watchTime += (newTime / NetworkSizeGossiper.networkSizeEstimate)
            profiles[key]!!.watched = true
        } else {
            profiles[key]!!.watchTime += time
            profiles[key]!!.watchTime /= 2
        }
    }

    fun updateEntryUploadDate(key: String, info: TorrentInfo) {
        addProfile(key)
        profiles[key]!!.uploadDate = info.creationDate()
    }

    fun incrementLikes(key: String) {
        updateEntryLikes(key, likes = 1, myUpdate = true)
    }

    fun incrementTimesSeen(key: String) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()
        profiles[key]!!.timesSeen += 1
    }
}
