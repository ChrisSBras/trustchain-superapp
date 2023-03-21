import os
import random
from torrentool.api import Torrent

from torrentool.api import Torrent
from .gossiper import Gossiper
from messages import TorrentPayload, MESSAGE_TORRENT_ID


class TorrentGossiper(Gossiper):
    def __init__(self, delay, peers, community):
        self.delay = delay
        self.peers = peers
        self.community = community
        self.torrent_list = os.listdir("torrents/")

    # sends random torrents to a set of peers
    def gossip(self):
        for p in self.community.get_peers():
            torrent_to_send = random.choice(self.torrent_list)
            torrent = Torrent.from_file(f"torrents/{torrent_to_send}")

            print(
                f"Sending torrent {torrent_to_send} with magnetlink: {torrent.magnet_link}"
            )

            packet = self.community.ezr_pack(
                MESSAGE_TORRENT_ID, TorrentPayload(torrent.magnet_link), sig=False
            )
            self.community.endpoint.send(p.address, packet)
