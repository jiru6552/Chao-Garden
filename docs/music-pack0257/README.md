> Current 0.25.11: five regional themes are already bundled from the supplied ADX files. This optional companion-pack workflow is only needed for overrides or separately supplied Black Market music.

# Area music

The mod automatically changes music between the Lobby, Neutral Garden, Hero Garden,
Dark Garden, Kindergarten, and the hidden Black Market. The classroom, clinic,
principal and Fortune-Teller rooms use the Kindergarten theme. Music changes after
entering the hidden passage, so the cupboard exterior gives no musical hint.

The included Neutral recording reuses the existing Garden CD made from the owner's
supplied Sonic Marketplace intro and loop. Its provenance is in
`art/music_asset_manifest.json`. It has not been independently identified as the
exact Sonic Adventure2 soundtrack rendition. The other five regional recordings
are not included. Empty slots leave ordinary Minecraft music available.

To add supplied recordings, place these Ogg Vorbis files in one directory:

| Filename | Area |
| --- | --- |
| `lobby.ogg` | Chao Lobby |
| `neutral.ogg` | Neutral Garden; optional replacement for the existing recording |
| `hero.ogg` | Hero Garden |
| `dark.ogg` | Dark Garden |
| `kindergarten.ogg` | Kindergarten and its public rooms |
| `black_market.ogg` | Hidden passage and Black Market |

Run `python3 scripts/build_area_music_pack_0257.py /path/to/recordings /path/to/Chao-Area-Music.zip`.
The helper includes only supplied files and writes the streaming sound definitions.
Put the resulting ZIP in Minecraft's `resourcepacks` folder and enable it. Partial
packs work; omitted regions stay unchanged. Reload resourcepacks to apply a change.
Full tracks should be Vorbis OGG, preferably stereo, with a clean loop boundary.
The helper checks the OGG/Vorbis header and ZIP integrity; it does not claim to identify
a song or evaluate its audible loop seam.

Each region uses `chao_garden:music.area.<area>` in the Music category. It loops,
fades when the selected area changes and respects Minecraft's Music volume.
Jukebox/Records audio temporarily ducks regional music. Leaving a realm, returning
to the title screen, or disconnecting stops the controller. The original Garden CD
remains a normal reusable jukebox record.

This directory supplies instructions and a builder; it does not contain unprovided
commercial soundtrack files.
