# Chao Garden 0.25.13 — fruit and gardening polish

This small follow-up completes the fruit/seed interaction review begun in 0.25.12.

- All native fruit and seed items sit higher and slightly smaller in both first-person hands. The watering can has its own inset so the spout fits the view.
- Chao retain their walking stride while carrying a seed. Their hands continue to support the real held stack.
- Planting and watering now navigate to the exact adjacent work spot. Vanilla's coordinate-based navigation had allowed stopping one block short, where a gardener could wait until it rejected the seed.
- The learned watering-can prop faces the plant, tips during the pour and lowers before the action ends. The watering effect still commits once at tick 40 of the 55-tick action.
- A Chao displaced beyond the existing three-block interaction reach cannot finish watering its old target remotely.

The fruit shapes, icons, hanging crops, effects, shop prices, tree lifespans and garden scenery from 0.25.12 are retained. No new game files are needed.

The live client review uses clearly marked disposable fixtures: actual item renderers, real Chao item offers, ordinary gardening AI and synced action progress. No client action or rig pose is forced. See [verification](../TESTING.md) for final checks and limits.
