# Chao Garden 0.25.17

Minecraft Java 1.21.1 · NeoForge 21.1.219 · Java 21

## Gardens

The Chao Garden replaces the former Transporter name and Garden Bell workflow. Place it inside an enclosure, open its UI and choose **Activate garden**. The scan covers the device's chunk and the eight surrounding chunks: a 3×3-chunk square. Fences, closed gates and full one-block walls enclose the area. It must contain a mature regular, Hero or Dark Garden fruit tree; a seedling or loose fruit does not count. Those three tree families no longer wither.

The six boxes register carried Chao by identity. Drag a carried Chao from the displayed inventory into an empty box; the real item remains in its inventory slot. Release that Chao inside its registered garden to make it a resident. Residents stay inside the saved area, including after a gate opens. Left-click the registered icon to unregister the Chao. A failed recheck preserves the previous valid garden. Moving a fence requires a successful recheck to update the saved area.

Gardens in the Neutral, Hero and Dark garden dimensions cover the entire authored garden without checking fences or trees. Registration is restricted to the garden's keeper and their own Chao, and the server checks the real carried item's identity.

## Artwork and journal

- A single karate mat places a complete 7×7 carpet centered on the clicked position. All 49 supported cells must be free. Breaking a tile recovers one mat and removes the assembled carpet. The texture comes from the supplied original karate-stage tatami atlas.
- The Chao music disc now uses a 16-pixel vanilla-style record silhouette with a Chao-blue label.
- The accordion's UVs now stay half a texel inside their own texture instead of sampling neighboring atlas sprites. Duplicate reverse item surfaces are also removed.
- Half-texel UV clamping fixes the coffin lid edge sampling a neighboring pale-blue atlas region.
- The Keeper's Journal has broad chapter categories, a section list, and search across chapter and section names. Index pagination uses arrows. Existing journals receive the updated content when opened.

## Tooltips and Omochao

Generic item instructions have been removed. Fruit planting space and Chao requirements, food/Chaos Drive/DNA benefits, possible DNA parts, short toy descriptions, lens effects and blue effect descriptions remain.

The Robo Heart explains that it rebuilds a cared-for Omochao at the next skill grade, up to S, with a wrench-trained Chao and three Parts Boxes. Calibrated hearts show the resulting grade and preserved creator ancestry.

Dead Omochao drop an Omochao Parts Box. A cared-for Omochao reaching the end of its life can still leave a Robo Heart. Old robot-component IDs resolve to Parts Boxes; old Garden Bell IDs resolve to Chao Gardens. Existing oversized garden rosters keep up to six registrations; overflow Chao bodies/items are not deleted.

Install the new JAR on both client and server, replacing the previous version. No separate resource pack is required. See `TESTING.md` for completed validation and limits.
