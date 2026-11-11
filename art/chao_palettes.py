"""Normal-coat color roles reconstructed from the official Sonic DLC references.

Type order is Normal, Swim, Fly, Run, Power.  These are authored approximations
of the visible color regions, not sampled Marketplace texture files.  Colored
and jewel genetic coats should continue to use the generator's inherited-coat
rules rather than replacing their base colors with these palettes.
"""
from __future__ import annotations

RGB = tuple[int, int, int]


def _palette(
    *,
    body: RGB,
    crest_tip: RGB,
    hand: RGB,
    toe: RGB,
    wing: RGB,
    belly: RGB,
    orb: RGB,
    head: RGB | None = None,
    tuft: RGB | None = None,
    heel: RGB | None = None,
    collar: RGB | None = None,
) -> dict[str, RGB]:
    """Create an independent palette with every supported material role."""
    return {
        "body": body,
        "head": head if head is not None else body,
        "tuft": tuft if tuft is not None else body,
        "crest_tip": crest_tip,
        "hand": hand,
        "toe": toe,
        "heel": heel if heel is not None else body,
        "wing": wing,
        "belly": belly,
        "collar": collar if collar is not None else body,
        "orb": orb,
    }


NORMAL_PALETTES: dict[str, list[dict[str, RGB]]] = {
    "neutral": [
        # Baby / normal: cyan front, pale-yellow caps and extremities, purple wings.
        _palette(
            body=(91, 208, 235), crest_tip=(246, 232, 137),
            hand=(246, 232, 137), toe=(246, 232, 137),
            wing=(200, 158, 222), belly=(91, 208, 235),
            collar=(84, 198, 229), orb=(249, 236, 107),
        ),
        # Swim: the DLC example is lime with cream cheek/chest bands and paddles.
        _palette(
            body=(155, 193, 30), head=(161, 196, 33),
            crest_tip=(55, 205, 176), hand=(242, 235, 151),
            toe=(225, 231, 143), heel=(170, 201, 65),
            wing=(58, 184, 155), belly=(237, 232, 153),
            collar=(236, 235, 159), orb=(39, 211, 179),
        ),
        # Fly: pink fins, blue/purple terminal blocks, blue hands and orange toes.
        _palette(
            body=(226, 164, 228), head=(230, 164, 234),
            tuft=(225, 154, 232), crest_tip=(91, 125, 236),
            hand=(102, 124, 230), toe=(232, 145, 75),
            heel=(171, 120, 215), wing=(73, 180, 97),
            belly=(226, 150, 87), collar=(236, 185, 237),
            orb=(249, 236, 107),
        ),
        # Run: green swept blades with cyan bars and grey-violet feet.
        _palette(
            body=(50, 193, 122), head=(43, 198, 129),
            tuft=(35, 195, 133), crest_tip=(42, 196, 204),
            hand=(53, 187, 127), toe=(140, 130, 192),
            heel=(104, 151, 153), wing=(49, 168, 174),
            belly=(230, 214, 96), collar=(36, 173, 104),
            orb=(43, 227, 118),
        ),
        # Power: orange blades with yellow borders and a rose-colored chest mark.
        _palette(
            body=(239, 151, 42), head=(243, 155, 43),
            crest_tip=(249, 224, 111), hand=(246, 219, 78),
            toe=(246, 220, 87), heel=(246, 230, 151),
            wing=(172, 130, 199), belly=(220, 100, 137),
            collar=(227, 132, 39), orb=(249, 184, 76),
        ),
    ],
    "hero": [
        # Hero: blue horn caps/extremities are separate from the yellow belly.
        _palette(
            body=(226, 239, 239), head=(237, 246, 245),
            tuft=(235, 244, 244), crest_tip=(61, 159, 228),
            hand=(66, 163, 225), toe=(65, 158, 222),
            heel=(231, 243, 241), wing=(244, 220, 115),
            belly=(245, 214, 77), collar=(174, 222, 234),
            orb=(65, 162, 232),
        ),
        # Hero/Swim is not shown in the supplied DLC slideshow.  This extends
        # the white Hero base with aqua paddles and cream/yellow chest marking.
        _palette(
            body=(225, 240, 239), head=(235, 247, 244),
            tuft=(220, 240, 236), crest_tip=(71, 189, 206),
            hand=(92, 198, 213), toe=(79, 183, 208),
            heel=(224, 243, 236), wing=(153, 221, 215),
            belly=(241, 222, 111), collar=(170, 226, 225),
            orb=(68, 205, 215),
        ),
        # Hero/Fly: purple-blue fork, purple collar, green belly and blue toes.
        _palette(
            body=(225, 231, 241), head=(238, 244, 247),
            tuft=(191, 158, 236), crest_tip=(87, 149, 238),
            hand=(80, 149, 226), toe=(83, 159, 231),
            heel=(233, 243, 242), wing=(172, 130, 220),
            belly=(105, 188, 131), collar=(177, 158, 224),
            orb=(248, 233, 111),
        ),
        # Hero/Run is not shown in the DLC slideshow; mint/green is an authored
        # extension of the observed white Hero and green Neutral/Run palettes.
        _palette(
            body=(225, 242, 232), head=(237, 248, 238),
            tuft=(194, 232, 208), crest_tip=(70, 186, 139),
            hand=(92, 200, 160), toe=(75, 192, 150),
            heel=(226, 242, 232), wing=(149, 220, 195),
            belly=(133, 205, 139), collar=(179, 229, 201),
            orb=(78, 217, 151),
        ),
        # Hero/Power: short coral forks, rose collar and matching red extremities.
        _palette(
            body=(237, 218, 229), head=(244, 241, 240),
            tuft=(232, 151, 173), crest_tip=(226, 104, 127),
            hand=(224, 102, 129), toe=(225, 103, 127),
            heel=(242, 221, 231), wing=(226, 145, 180),
            belly=(225, 117, 145), collar=(232, 159, 185),
            orb=(248, 156, 208),
        ),
    ],
    "dark": [
        # Dark: charcoal body, coral terminal cap, gloves, toes and chest crescent.
        _palette(
            body=(45, 49, 52), head=(47, 54, 59),
            tuft=(29, 36, 39), crest_tip=(228, 95, 101),
            hand=(225, 93, 100), toe=(226, 95, 101),
            heel=(47, 51, 54), wing=(139, 61, 82),
            belly=(225, 97, 106), collar=(73, 49, 55),
            orb=(238, 88, 94),
        ),
        # Dark/Swim: bright cyan jaw, chest and paddles against charcoal.
        _palette(
            body=(43, 49, 53), head=(44, 52, 57),
            tuft=(49, 172, 222), crest_tip=(62, 165, 234),
            hand=(53, 177, 231), toe=(51, 180, 236),
            heel=(44, 52, 56), wing=(66, 174, 218),
            belly=(46, 177, 228), collar=(57, 185, 231),
            orb=(57, 79, 237),
        ),
        # Dark/Fly is not depicted in the DLC slideshow.  Violet terminal
        # blocks and wings are an authored extension, not an exact DLC palette.
        _palette(
            body=(45, 45, 55), head=(48, 48, 59),
            tuft=(103, 69, 138), crest_tip=(169, 105, 213),
            hand=(150, 91, 192), toe=(146, 89, 189),
            heel=(47, 46, 58), wing=(170, 104, 217),
            belly=(157, 99, 199), collar=(93, 62, 124),
            orb=(185, 104, 230),
        ),
        # Dark/Run: green lateral fins/chest with cyan bars and a blue cube.
        _palette(
            body=(43, 53, 49), head=(41, 54, 52),
            tuft=(24, 165, 90), crest_tip=(39, 195, 171),
            hand=(30, 174, 108), toe=(29, 161, 82),
            heel=(43, 54, 48), wing=(30, 175, 127),
            belly=(32, 176, 106), collar=(37, 102, 72),
            orb=(35, 85, 239),
        ),
        # Dark/Power is not depicted in the DLC slideshow.  Warm red/orange
        # regions extend the observed charcoal Dark and orange Power designs.
        _palette(
            body=(53, 43, 45), head=(56, 45, 47),
            tuft=(160, 62, 61), crest_tip=(230, 108, 71),
            hand=(205, 85, 68), toe=(200, 83, 68),
            heel=(55, 43, 45), wing=(163, 62, 65),
            belly=(222, 92, 73), collar=(104, 48, 48),
            orb=(241, 116, 74),
        ),
    ],
}


def palette_for(alignment: str, typ: int) -> dict[str, RGB]:
    """Return a mutable copy; modifying it never changes the shared presets."""
    return NORMAL_PALETTES[alignment][max(0, min(4, int(typ)))].copy()


# Canonical main Chaos forms. Genetic coat data remains in the saved Chao;
# these dedicated authored materials make each immortal form recognizable.
CHAOS_PALETTES = {
    'light': _palette(body=(180, 230, 232), head=(190, 237, 237),
        tuft=(66, 175, 211), crest_tip=(42, 149, 214),
        hand=(51, 164, 216), toe=(203, 239, 229), heel=(171, 221, 225),
        wing=(125, 213, 234), belly=(229, 241, 219), orb=(253, 237, 109)),
    'angel': _palette(body=(241, 238, 227), head=(246, 244, 232),
        tuft=(239, 236, 226), crest_tip=(237, 149, 192),
        hand=(237, 157, 196), toe=(239, 170, 205), heel=(240, 235, 225),
        wing=(247, 225, 146), belly=(224, 192, 216), orb=(252, 229, 110)),
    'devil': _palette(body=(78, 37, 51), head=(73, 34, 48),
        tuft=(101, 39, 52), crest_tip=(248, 147, 62),
        hand=(242, 107, 49), toe=(208, 66, 61), heel=(83, 37, 50),
        wing=(172, 86, 140), belly=(220, 83, 87), orb=(182, 104, 246)),
}
