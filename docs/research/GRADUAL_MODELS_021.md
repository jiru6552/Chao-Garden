# Gradual model and coat rendering — 0.21

The model now has three independent coordinates: the fixed first adult family,
the two reversible opposing training axes, and evolution strength. Each of the
fifteen alignment/first-type rigs contains an authored young adult base plus
nine topology-compatible fully aged targets. Strength zero renders the young
base regardless of the secondary sliders; strength 0.6 is halfway, and strength
1.2 reaches the endpoint. This is a continuous deformation, without a second
cocoon or threshold mesh replacement. Baby hints use only 14% of the normalized
morph. Ordinary sliders remain in −1…+1; optional Monster extension is separate.

The previous universal giant Swim/Fly/Run/Power ridges and enlarged core heads
were removed. The artist inspected all150 source photos (15young and135aged)
from these references, including their young/fully-aged pairs:

- https://chao-island.com/gallery/advanced-evolution/neutral.html
- https://chao-island.com/gallery/advanced-evolution/hero.html
- https://chao-island.com/gallery/advanced-evolution/dark.html
- https://chao-island.com/wiki/Gradual_Evolution

The authored families use distinct silhouettes: Neutral Normal has one small
curl; Swim develops swept/drooping fins; Fly has paired jester points; Run uses
swept quills; Power changes its compact crown and body bulk. Hero families use
buds, petals, long feelers and split stems. Dark families use a rear curl,
backward fins, paired horns, swept quills and a curved central crest. The mixed
secondary corners interpolate only the allowed cross-axis pairs. A fixed Swim
first type can still mature toward Fly, because the first and second categories
are separate. No Swim+Fly or Run+Power secondary mixture is introduced.

The adaptations retain the supplied block-built aesthetic. Curves consist of
overlapping axis-aligned cuboids. The base skull changes by at most11%, its
front and lower planes stay fixed, and source eye panels/iris geometry and all
semantic joints remain fixed. Body bulk, hands, toes and wings change where the
family references call for it. Hats hide both donor and authored crests while
leaving the real skull and eyes visible. Current morph sampling is shared by
rendering, attachment bounds, shape fingerprints and toe reach. The model primes
this entity's coordinates before gait fitting, avoiding stale geometry from a
previously rendered entity.

`GradualCoatMaterial` blends an authored natural body/accent/wing palette using
the same axes and normalized age. This provides the green young Run → blue
mature Sonic-like form, dark Run red markings, and bright mature flight wings.
Colored and jewel genetic coats keep their inherited pigment. Eye texels and
source alpha are protected. Negative/Bright/Hero Brush effects run afterward on
a copy, so they cannot mutate a shared aged texture. Omochao can sample the same
natural palette on its own baby-layout UVs before applying its mechanical
hardware and source palette effects.

The first in-game review exposed thin, nearly hidden flight feelers and comb-like
running quills. The corrected shapes have raised curved jesters and broad,
tapering quill clusters at three heights; core proportions remain restrained.
Wing UVs form their own protected region, so mature NiGHTS wings now receive the
yellow palette even where those texels lie outside the main skin mask.

A second complete review of the15 young/mid/aged front and side triads and15
aged grids caught a different square-skull problem: the Swim petals were
connected correctly but embedded in the solid core. Hero Swim and Neutral Swim
now form an exposed outside shoulder before their downward tips. Dark Fly's
paired jesters and Swim petals receive the same outside clearance; Hero Fly's
four mature fine strands sweep visibly beyond the head. Each family keeps its
own directions and thickness. Independent checks now require real exposed
blocks and downward length relative to the core, in addition to connectivity.

The inherited Swim atlas used by the Hero/Swim alias contained a teal head band.
That natural Hero core alone now uses one authored white swatch in a proven
unused atlas texel. Its original eyes and all inherited colored/jewel UVs remain
unchanged. Negative/Bright effects are applied once to this isolated swatch.
This adds the intentionally used swatch alpha; existing rendered source pixels,
including eyes and original transparency, remain protected.

The135 target palettes are authored colors, not copied photo pixels. External
reference photos and full gallery text are internal research inputs and are not
included in the release. All41 supplied asset-only raw inputs remain untouched.

Independent checks cover all135 actual aged geometry signatures, young-axis
invariance, continuous age interpolation and reversal, excluded opposing
weights, connected noncollapsed stepped geometry, restrained main skull bounds,
fixed source anchors/eyes/front plane, palette coverage and repeat generation.
The existing independent hierarchy/UV/animal-part validation remains active.
Actual-client probes additionally check moving attachment transforms at three
ages and actual selected texture pixels, including inherited coats, eyes/alpha,
cache isolation and the builder-colored robot path. Actual in-game views are
required to assess final visual appearance; these numerical checks do not claim
exhaustive collision freedom under every animation or equipment combination.
