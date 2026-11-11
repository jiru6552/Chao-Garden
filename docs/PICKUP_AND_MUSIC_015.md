# Held Chao and garden CD — 0.15

Sneak-right-clicking an owned Chao with an empty hand picks up that same creature. Its held inventory item stores its UUID, name, owner and complete Chao state; holding it uses the animated Chao renderer, and eligible adults still fit the Accessories companion slot.

Releasing it now consumes the held item completely in both survival and creative. A blocked or rejected release keeps the saved Chao item intact. No empty container is returned. The old `chao_carrier` registry ID is retained only so existing filled items remain usable; it is hidden from creative, has no crafting recipe, and leftover empty items from older inventories disappear when their inventory tick runs.

The **Chao Garden CD** is a normal Minecraft 1.21.1 jukebox item. Craft it with eight iron nuggets around an emerald. It plays the Neutral Garden introduction followed by one full garden loop, for **115.809592 seconds**. Vanilla jukebox insertion, completion, ejection and comparator output work normally; its comparator strength is 7. The disc is reusable.

The recording was assembled from `Chao-Garden-Music-Neutral_intro.ogg` and `Chao-Garden-Music-Neutral_loop.ogg` in the owner's supplied official Sonic the Hedgehog Marketplace world pack. The arrangement contains one intro and one loop; no automatic infinite looping is added to jukeboxes. The original recordings are from that Gamemode One / SEGA pack. `art/music_asset_manifest.json` records source hashes, the derived track hash and measured duration. The CD item artwork is newly authored native 16-pixel art.

`GardenMusicContent` uses the vanilla `JUKEBOX_PLAYABLE` component and the data-driven `jukebox_song` registry. `scripts/generate_music_assets.py` regenerates the sprite, recipe, song metadata and merge fragments. Rebuilding the included audio requires FFmpeg and FFprobe plus the two source recordings; ordinary Gradle builds use the existing Ogg asset.

Validation includes actual server GameTests for survival and creative release, blocked-placement recovery, duplicate-UUID rejection, legacy empty-item cleanup and preservation of filled items. The music test inserts the CD through vanilla jukebox logic, verifies its song and comparator strength, advances the normal song player to its end boundary, then verifies exactly one disc ejects. See the release validation report for the run results.
