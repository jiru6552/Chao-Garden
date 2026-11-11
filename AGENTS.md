# Chao animation testing

When adding or revising a toy or animation, include an operator-accessible preview command with one-shot playback, looping and stop controls. Keep `/chao testtoy list` and command suggestions current. Reuse the production animation clock, pose and sound/particle cue implementation; do not create a separate demonstration animation. Hand-held toys register through `HandToyEffects`; other activities need an appropriate preview adapter and cleanup.

Preview commands must preserve saved care, genetics, lessons, age and real items, grant no gameplay rewards, restore temporary AI flags, and stop cleanly on interaction, damage, unload, disconnect and explicit stop. Keep the current renderer. Test commands through the native dispatcher and actual clients; do not treat automated assertions as a visual or listening pass. Include command examples and remaining listening/multiplayer limitations in each build report.
