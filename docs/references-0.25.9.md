# Hero Garden and audio reference review

The source release's Battle overview and two official SEGA screenshots were opened and visually inspected again. Their original files and hashes remain in references-0.25.3/hero/. The official page was also retrieved during this continuation.

- [SEGA / Sonic Team: Chao World screenshots](https://sonic.sega.jp/sonicadv2/0601/shots_e.html).
- [Battle overview context](https://chao-island.com/wiki/Hero_Garden).
- [X-Hax original ADX music list](https://raw.githubusercontent.com/wiki/X-Hax/SA2BModdingGuide/ADX-Music-List.md), retrieved directly to verify the audio identifiers.

Visual findings: the Battle overview shows a shallow pavilion cap, thin pale lavender columns, restrained fountain top, pool on the fountain side, and open grass toward the opposite exit. The existing pool/lawn/landmark topology already follows this broad layout. This pass changes the pavilion column proportions and two roof silhouettes. The Minecraft scale and shapes remain an interpretation, not a measured one-to-one reconstruction.

The official 2001 images are Dreamcast-era references, so their topology is not treated as proof of Battle-only geometry. The Battle overview is the reference for the pavilion and broader layout. In particular, this update does not add the Dreamcast river/bridge.

Video searches were attempted but no additional playable Hero Garden walkthrough was inspected in this continuation. The geometry changes are supported by inspected still images. No claim of exhaustive video-based comparison is made.

The ADX list identifies CHAO_HALL as the Battle lobby, CHAO_G_BGM_H/D/N as the gardens, CHAO_K_M as kindergarten, and six CHAO_K_SING_HERO/DARK tracks. The importer does not equate those six tracks with the separate learned solo voice samples. CHAO_K_M2's role is not specified in that list and is therefore not mapped to Black Market.

No original SA2 audio was obtained or listened to in this run. Audio conversion and playback checks use clearly labelled synthetic fixtures; those checks establish import, decoding, timing and cleanup behavior, not authenticity of recordings that have not been provided.
