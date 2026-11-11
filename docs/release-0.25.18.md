# Chao Garden 0.25.18

Minecraft Java 1.21.1 · NeoForge 21.1.219 · Java 21

## Chao Analyzer

The item is now a thin blue tablet with yellow corner guards and a Chao status display. Its front, rear and side surfaces are modeled for inventory, held and dropped views.

The preview is larger and follows the mouse horizontally and vertically while the cursor is inside its panel. The head turns relative to the body. Rendering restores all current and previous head/body/pitch values, so inspecting a Chao does not change its world pose.

**Stats** replaces Status; **Favorite Fruit** replaces Favorite. Grades use E brown, D orange, C green, B blue, A purple and S gold/yellow.

## Genes

- **Appearance:** compares what the Chao shows now with the coat, pattern, shine, jewel and favorite-fruit options it can pass on. Each baby receives one option from each parent; carried traits can be hidden in the parent's appearance.
- **Skill Genes:** compares current grades with inheritable grades. Higher grades improve points gained per level. Intelligence and luck show their inherited potential on the existing 0–255 scale. Current levels and points do not pass to offspring.
- **Growth:** names each direction and shows how far the marker leans from the middle. Appearance/adult subtype directions are separate from first evolution, which uses highest stat points, with Stamina representing Normal and the latest increase breaking ties.

## Parts

An outlined Chao has labeled leader lines for ears, arms, feet, tail, head tuft, horns, face and wings. Labels report currently visible parts, with explicit handling for mechanical bodies and ghost-hidden limbs.

The Chaos requirements remain below: third life, all 21 original animal DNA gifts, happiness above 80 and Normal first evolution. Click **Animal DNA** to see received and missing animals, then use **Missing only** to filter the list. Receiving DNA and displaying its parts are tracked separately. Bonus DNA cannot substitute for the original 21. Adult Chao are reminded that these conditions are checked when a child evolves.

## Daycare lessons

The analyzer shows learned skills and daycare instructions. It no longer offers Join Current Class or manual lesson cancellation. Only registered Kindergarten daycare students can start or advance a lesson, including through server requests. A failed drop-off releases its reserved seat and preserves the original Chao/carrier.

Previously learned skills and earned instrument rewards remain intact. An old unfinished freestanding lesson stops safely on its next loaded tick. Outstanding earned instruments can still be claimed from the owned nearby Chao without requiring a desk. Old desk IDs remain loadable for existing worlds and internal school anchors; they no longer start lessons, have no recipe/creative listing, and drop no new desk item.

Replace the old JAR with this version on both client and server. No separate resource pack is required. Previous garden, journal, tooltip, music and artwork changes remain included. Completed checks and practical limits are in TESTING.md.
