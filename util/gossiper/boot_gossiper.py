from .gossiper import Gossiper
from messages import MESSAGE_BOOT_REQUEST, MESSAGE_BOOT_RESPONSE
from pyipv8.ipv8.messaging.serialization import Serializable
from pyipv8.ipv8.lazy_community import lazy_wrapper_unsigned


class BootRequesPayload(Serializable):
    def __init__(self, message):
        self.message = message

    def to_pack_list(self):
        return [("Q", MESSAGE_BOOT_REQUEST), ("raw", self.message.encode("UTF-8"))]

    @classmethod
    def from_unpack_list(cls, *args):
        return cls(*args)

class BootResponsePayload(Serializable):
    def __init__(self, message):
        self.message = message

    def to_pack_list(self):
        return [("Q", MESSAGE_BOOT_RESPONSE), ("raw", self.message.encode("UTF-8"))]

    @classmethod
    def from_unpack_list(cls, *args):
        return cls(*args)

class BootGossiper(Gossiper):

    def __init__(self, delay, peers, community, network_gossiper):
        self.max_loops = int(30 / delay)
        self.delay = delay
        self.peers = peers
        self.community = community
        self.network_gossiper = network_gossiper
        self.running = True

    def gossip(self) -> None:
        if not self.running:
            return 
        if self.max_loops > 0:
            self.max_loops -= 1

            for peer in self.community.get_peers():
                packet = self.community.ezr_pack(
                    MESSAGE_BOOT_REQUEST, BootRequesPayload(""),
                    sig=False
                )

                self.community.endpoint.send(peer.address, packet)

    def received_request(self, peer, _payload) -> None:
        message_map = {"NetworkSize": self.network_gossiper.estimated_size}
        message_to_send = self.serialize_message(message_map)

        packet = self.community.ezr_pack(
            MESSAGE_BOOT_RESPONSE, BootResponsePayload(message_to_send),
            sig=False
        )

        self.community.endpoint.send(peer, packet)

    def received_response(self, _peer, payload: bytearray, data_offset=31) -> None:
        #TODO: cleanup
        result = payload[data_offset:].decode()
        result_map = {}

        for i in result.split(","):
            split = i.split("~")
            result_map[split[0]] = int(split[1])

        self.network_gossiper.estimated_size = result_map["NetworkSize"]
        print(f"BOOTRESPONSE {result}, size set to {self.network_gossiper.estimated_size}")

        self.running = False

        