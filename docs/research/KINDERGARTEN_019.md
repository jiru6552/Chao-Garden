# Kindergarten services and source decisions — 0.19

The source priority is SA2/SA2B, then SA1/SADX where needed, then explicitly selected CWE additions. References inspected:

- https://chao-island.com/wiki/Classroom
- https://chao-island.com/info-center/kindergarten/classroom.html
- https://chao-island.com/wiki/Chao_World_Extended
- https://chao-island.com/wiki/Personality_%26_Emotion
- https://chao-island.com/info-center/kindergarten/health-center.html

The modern Classroom Wiki lists a 30-minute offer and ten-minute lesson. The older Info Center lists 60/30. This release follows the newer Wiki. Both list eight original instruments, four dances, exercise, song and drawing; song/drawing improve to five. The Info Center explicitly says classroom attendance grants no Intelligence increase. The original ordered cycle is retained, with CWE Triangle/Guitar/Accordion appended. Details not established by these sources are not presented as exact original equations.

## Minecraft implementation

`DoctorContent` registers an original cabinet-style Doctor Station, book-and-wood Lesson Desk and a medicine bottle. Both blocks have four native horizontal facings, collision, recipes and recoverable block loot. They are physical service locations rather than a virtual action usable anywhere.

`ChaoCareNetwork` sends bounded UUID/action/value requests. The server resolves the actual loaded Chao in the player's dimension and validates keeper permission, eight-block player distance, creature availability, nearby station, actual inventory and eligibility. Clients supply no trusted price, stat delta, item stack, dye colour or unlock bit. Deliberate creative ownership overrides follow the existing mod rule.

One real medicine treats all six active illnesses at a Doctor Station. Offering the medicine uses the same delayed considered-gift transaction as other care supplies. The remedy is an authored Minecraft item; the source's clinic is not claimed to charge this item. Paid grade care follows CWE eligibility and ring costs; emerald conversion is the existing 100-rings-per-emerald adaptation. A rejected upgrade charges nothing, including if bounded trait storage refuses its commit.

The lesson desk requires healthy, awake students with sufficient fullness and has configurable four-student capacity. Lessons advance in loaded entity ticks and pause when the real desk is missing/out of range, the student is ill/asleep/busy, a contest controls it or it is an equipped companion. Missing/unloaded time never grants knowledge. Cancelling a class and retaining accumulated progress on a temporary interruption are Minecraft usability adaptations; the source classroom does not allow early withdrawal. Rebirth cancels unfinished lessons while learned knowledge survives.

A pending lesson stores its ID, remaining ticks, physical desk position and paused flag. Completion clears its pending transaction before granting one knowledge level. Reloading a completed save cannot repeat it. First instrument learning records a one-time physical prop claim; the nearby keeper receives it, or a pending claim waits for that keeper to return to a desk. These physical prop rewards provide acquisition for instruments that lack a source shop price. They are an explicit Minecraft adaptation, not an invented canonical merchant cost.

## Doctor chart and actual equipment

The live chart has Status, Genes, Parts, Mind and Care pages. Mind shows all thirteen sliders, eighteen emotion values, six illness diagnoses and a bounded two-generation family record. Doctor descriptions cycle among the profile's eligible labels. Family records include two parents and four grandparents when known; unknown ancestors remain unknown. Displaying a chart never rerolls saved genes.

Care separates clinic services, timed lessons and wardrobe transactions. Five Chao equipment categories hold actual native ItemStacks; only the player Companion slot needs the optional Accessories mod. Equipping consumes one inventory unit even in creative, returns previous equipment once and keeps custom components. Dye consumes an actual dye, and identical tint is a no-op. Native equipment save/sync preserves colours and names across carrying and reload. Earned medal flags gate selection; selection never grants an award.

Default classroom settings are `lessonTicks=12000`, `lessonOfferTicks=36000`, `classroomCapacity=4`. All are server settings. The gameplay clock follows server tick rate rather than wall-clock catch-up.

## Verification boundaries

`KindergartenGameTests` covers real station and inventory requirements, ownership/range rejection, one-unit medicine, classroom completion and pause/reload behavior, grade payment/change and allele conservation, wardrobe component transfer, dye, once-only instrument claims and earned-medal selection. Final executed results belong in the release's TESTING.md rather than being inferred from test source.
