# Classroom vocals and area music

The two classroom lesson pupils now alternate short **voiced melodies**, rather
than the ordinary ambient reaction. Each six-second cycle has two2.8-second
phrases with a short rest between them. Six syllables per phrase follow the note
particles. The sounds use Minecraft's ordinary localized entity sound playback,
at volume0.75, with a12-block attenuation distance and mono Vorbis samples.
Only an actual seated pupil currently performing the song lesson can emit them.
Repeated processing of the same pupil in the same server tick cannot duplicate
the voice or note particles.

These are **new, original call-and-response arrangements** made by pitching and
sustaining the already provided Chao call timbre. They are not the exact SA2
classroom singing sample. The source call, all four other reaction recordings,
and the supplied Neutral Garden CD remain byte-for-byte unchanged. The source,
musical notes, output hashes and preservation set are recorded in
`art/school_singing_manifest_0258.json`; the score and deterministic Ogg encoding
are reproduced by `scripts/generate_school_singing_0258.py`.

The full provided source was audited for additional audio: it contains those five
short reactions and the existing115.81-second Neutral Garden recording. It has no
additional singing recording or regional soundtrack files. Consequently the
Chao Lobby, Hero Garden, Dark Garden, Kindergarten and Black Market original
music recordings remain pending. Their existing regional routing and optional
resource-pack builder remain ready for supplied audio; no unrelated track is
substituted. The exact SA2 rendition of the provided Neutral music is unverified.

The audio guard decodes both generated phrases and checks their actual pitched
syllables, nonzero levels, quiet rests, codec, duration, source preservation,
sound mappings and provenance. It rejects reaction substitution, false game
sample attribution, flattened melodies, mistuned notes and overlapping starts.
Native client playback observation is a separate runtime check; a null OpenAL
sink alone does not establish an audible listening review.
