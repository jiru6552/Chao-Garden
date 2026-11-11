# Chao Garden 0.25.9

This reconstructs the interrupted post-0.25.8 update from the saved complete project.

The Hero pavilion now has four slim lavender columns with separate base, shaft and capital members. Native collision follows their clipped corners. Its cap has two low tiers instead of four tall tiers. The fountain retains its water curtain, emblem and cornice, with a shallow top replacing the tall stepped pediment. The existing pool, lawn, exit, transporter, bell island and care systems remain. Authored room revision 34 applies the scenery changes through the existing migration path.

A local SA2 audio importer identifies the Battle lobby, Neutral/Hero/Dark garden and kindergarten recordings and six Hero/Dark singing tracks. It converts them to a normal Minecraft resource pack, with an import report recording source filenames, hashes and decoded duration. It refuses missing requested area recordings by default, ambiguous filenames and invalid audio. It leaves an existing output intact when conversion fails.

The optional full-length classroom chorus plays one stream at a time while the actual ambient pupils are singing. It waits for the recording to finish, leaves a short pause, then selects another. It stops on leaving the classroom or ending the song lesson. Existing short arranged phrases remain the fallback without imported audio. These changes do not implement the distinct learned solo singing repertoire.

Original SA2 recordings are not bundled in this release. The music files supplied with the project still contain only the previously supplied Neutral Garden recording. See SA2-AUDIO-IMPORT.md to import from a local installation. Black Market audio remains separately supplied; the unidentified CHAO_K_M2 file is not assumed to be the shop theme.

Reference inspection and limitations are in references-0.25.9.md. Current checks and logs are in validation-0.25.9/. No new preview gallery is included.
