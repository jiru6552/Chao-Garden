# Chao voice recordings

Version 0.4.0 replaces the Allay placeholder voices with a small collection of
Sonic Adventure-series Chao recordings. These are archived game recordings, not
newly synthesized voices, and are not a rip of the Bedrock DLC. Audio ownership
remains with SEGA / Sonic Team; the project's original-code/visual-asset permission
does not apply to these recordings.

## Included recordings

| Mod asset | Archived source | Current use |
| --- | --- | --- |
| `chao/call.ogg` | **Chao**, Sonic Adventure 2 soundboard, sound 29640 | Petting response and completed evolution |
| `chao/chat_1.ogg` | SADX `Chao Voices/B05_00_75.WAV` | Ambient chatter variation 1 |
| `chao/chat_2.ogg` | SADX `Chao Voices/B05_00_76.WAV` | Ambient chatter variation 2 |
| `chao/chat_3.ogg` | SADX `Chao Voices/B05_00_77.WAV` | Ambient chatter variation 3 |
| `chao/startled.ogg` | SADX `Chao Voices/B05_00_46.WAV` | Hurt reaction and death reaction |

The Chao call is identified by the hosting soundboard as a Sonic Adventure 2 clip.
The other four clips are from **Chao**, Sonic Adventure DX: Director's Cut
(PC / Computer), archived on The Sounds Resource by **Flandre9999vr**, submitted
December 30, 2015.

### Sources

- [Chao — Sonic Adventure DX archive](https://sounds.spriters-resource.com/pc_computer/sonicadventuredxdirectorscut/asset/399231/)
- [Chao — labeled Sonic Adventure 2 sound clip](https://www.101soundboards.com/sounds/29640-chao)
- [Sonic Adventure 2 Chao soundboard](https://www.101soundboards.com/boards/10529-chao-sounds-sonic-adventure-2)
- [SADX voice-bank file names and IDs](https://sadocs.unreliable.network/wiki/Sonic_Adventure/Sound_Effects)

## Cue selection and its limits

The source archive uses numbered file names instead of descriptive emotional
labels. To avoid arbitrary selection, the ambient set follows the Chao World
Extended chatting code, which selects voice indices 75–94 and contains its older
`VOICEBANK5(75)` mapping in a comment. Only the first three are included here.

The startled cue follows that project's fear behavior using sound 24622
(`TONE(6,46)`). Mapping that voice number to SADX bank 5 file 46 is an inference
from the shared voice-bank numbering. This is a fear/startle recording chosen for
our hurt/death reaction; it is not a claim that either original Adventure game
used this exact recording as a death sound. Death currently reuses that same
recording at 0.95 pitch. There is no separately claimed hungry/crying recording.

Reference code (used only to identify cues; no source copied into this mod):

- [CWE chatting behavior](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_behavior/alsbhv_chat.cpp)
- [CWE fear behavior](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_behavior/albhv_fear.cpp)

## Processing and playback

The source recordings were decoded to mono at 22,050 Hz, trimmed only at their
leading/trailing quiet margins, given a 4 ms edge fade, peak balanced, and encoded
as Ogg Vorbis at quality 5. No time stretching, synthetic voice replacement,
gameplay commentary, or background music is included. Source/output SHA-256 hashes,
clip durations, and applied gain are in `audio-provenance.json`.

Minecraft positional playback uses mono audio. Ambient chatter has three randomly
selected variations and a 180-tick ambient interval; it is suppressed during
sleeping, cocooning, eating, and the petting reaction. Child Chao do not receive
Minecraft's normal doubled baby-voice pitch because the recordings already have
the Chao timbre. Petting synchronizes an actual 40-tick visual reaction to clients.

The existing vanilla eating crunch remains a physical food sound. All new voice
events have subtitles and can be replaced independently by a resource pack through
`sounds.json` without changing inherited Chao data.

Validation covers decodable OGG resources, mono channels, audible nonempty sample
data, bounded duration/peak levels, sound-event/resource/subtitle links, and Java
compilation. The clips were selected using archival labels and documented bank
indices, not a claim of independent perceptual listening verification.
