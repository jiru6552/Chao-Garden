# Chao Garden 0.25.16

Both exterior Kindergarten dormer windows are recessed into the actual gable wall. Their two-by-two apertures, wall backing and sills now line up; the adjacent roof no longer obscures the lower frame. Scenery revision 38 applies the correction through the existing world migration path.

The supplied Chao World Extended 9.6.2.2 files now supply models and materials for 26 items already registered in this mod:

- Ten wearables: baseball cap, bow tie, round glasses, aviators, headphones, pacifier, press hat, blue hoodie, pink hoodie and Cheese bow tie.
- Two toys: guitar and accordion, including their existing placed/play states.
- Five fruits: Shiny Fruit and the Power, Run, Fly and Swim Hyper Fruits, including their four growing-pod stages.
- Nine gifts: Suspicious Potion, Reverse Mirror, Omochao Parts Box, Cosmetic Wings and the five Hero brushes.

Inventory icons are rendered from the corresponding models. Wearable source surfaces are adapted to the mod's Chao attachment landmarks. Glasses retain separate hinges; the headphone band is adapted to join the source ear cups around the skull. Hoodie bodies, sleeves and hoods follow the blockier skin envelope while retaining source UVs. The hood appears when no head-slot garment is equipped. Source rest-pose rotations are corrected for display and attachment. These are Minecraft adaptations, not a port of CWE's executable or its complete item collection.

Existing item identifiers, prices, acquisition, dye support and effects remain in place. The earlier garden texture reversions, Kindergarten paintings, garden banks, original toys/hats and bundled music remain included.

## Sources and rebuilding

`art/cwe02516/` contains selected supplied models, configurations, archives, decoded textures, selected PE object addresses, prior fitting envelopes and input hashes. `art/extract_cwe_02516.py` reads these files offline; it does not execute the supplied DLL. `scripts/generate_cwe_assets_02516.py` runs as the final source-asset pass in the normal pipeline, followed by resource validation. No source game or CWE installation is required at runtime.

CWE credits, as listed in the supplied `mod.ini`: Darkybenji, Exant, Mindacos, CGBuster, Shaddatic, Krzys2, Nostalgia Ninja, AWildDaydreamer, Justin113D, Erubbu and Soep Hond. Project: https://github.com/cweteam/cwe_main . Binary-format references: https://github.com/X-Hax/sa_tools/tree/master/Libraries/SAModel and the SA Tools PAK implementation.

Replace the previous mod JAR on both client and server. Validation evidence and practical limits are in `TESTING.md`.
