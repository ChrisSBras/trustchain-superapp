package nl.tudelft.trustchain.detoks.gossiper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.TorrentManager

class WatchTimeGossiper(
    override val delay: Long,
    override val peers: Int,
    private val blocks: Int,
    private val context: Context

    ) : Gossiper() {

    override fun startGossip(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                gossip()
                delay(delay)
            }
        }
    }

    override suspend fun gossip() {
        val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

        val randomPeers = pickRandomN(deToksCommunity.getPeers(), peers)
        val randomProfileEntries = pickRandomN(
            TorrentManager.getInstance(context).profile.magnets.entries.map { Pair(it.key, it.value.watchTime) },
            blocks
        )
        if (randomPeers.isEmpty() || randomProfileEntries.isEmpty()) return

        randomPeers.forEach {
            deToksCommunity.gossipWith(
                it,
                GossipMessage(DeToksCommunity.MESSAGE_WATCH_TIME_ID, randomProfileEntries),
                DeToksCommunity.MESSAGE_WATCH_TIME_ID
            )
        }
    }
}
