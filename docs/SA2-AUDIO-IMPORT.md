# Supplied SA2 audio and optional importer

Version 0.25.11 already includes all five area themes and six full classroom singing recordings from the supplied ADX.zip. Replace the mod JAR to use them; no resource pack is required. The selected source files and conversion report are in art/audio02511/.

The importer remains available if you want to build an optional overriding resource pack from a different local installation:

1. Install Python 3 and FFmpeg, including ffprobe.
2. Open a terminal in this project's source folder.
3. Run the following command, replacing the game path with yours:

```powershell
py scripts/import_sa2_audio_0259.py "C:\Program Files (x86)\Steam\steamapps\common\Sonic Adventure 2" "Chao-SA2-Audio.zip"
```

4. Put Chao-SA2-Audio.zip in Minecraft's resourcepacks folder and enable it above other Chao sound packs.
5. Use Chao Garden 0.25.11 on Minecraft Java 1.21.1 / NeoForge.

You can pass the game's resource/gd_PC/ADX folder directly instead. On macOS/Linux, use python3 instead of py. The game files are read without modification. The 0.25.11 project contains only the eleven selected supplied recordings and their converted outputs.

| Destination | SA2 filename |
| --- | --- |
| Battle lobby | CHAO_HALL.ADX |
| Neutral Garden | CHAO_G_BGM_N.ADX |
| Hero Garden | CHAO_G_BGM_H.ADX |
| Dark Garden | CHAO_G_BGM_D.ADX |
| Kindergarten | CHAO_K_M.ADX |
| Classroom singing | CHAO_K_SING_HERO1–3.ADX and CHAO_K_SING_DARK1–3.ADX |

The lobby, Hero, Dark and kindergarten files are required by default. Neutral and the singing files are imported if present. Add --allow-partial only if you want a pack with missing area recordings. Read the command's report to see what was included.

The importer uses complete singing recordings, converted to mono for positional playback. One classroom stream replaces the arranged fallback while the lesson is active. This is not an import of every individual Chao's learned solo singing sound. The jukebox disc is not replaced, and no unverified Black Market mapping is added.

Filename source: [X-Hax SA2B Modding Guide](https://github.com/X-Hax/SA2BModdingGuide/wiki/ADX-Music-List).
