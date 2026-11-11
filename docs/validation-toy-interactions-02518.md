# Toy interaction follow-up on the 0.25.18 source

See also `validation-mobile-toys-02518.md` for the subsequent ball/horse/lifesaver chunk and expanded check totals.

Status: **implemented; native build and visual verification pending**. This working-source change is not represented by the previously packaged 0.25.18 JAR or its historical PASS records.

## Changes

- Piano and organ approach their authored north/key side, rotated with block facing. Final movement reaches a precise, actor-width-aware contact point before play begins. During play, bodies face the keyboard; displacement or blocked contact cancels rather than playing remotely.
- Keyboard collision bounds follow the shipped source meshes, including the organ's narrow depth. Keyboard path nodes remain fence-like to avoid climbing onto cabinets. Small seated key presses replace the generic alternating arm wave.
- Coffin: one opening, a held-open pretend nap, exit, then closing. The shallow original prop remains open while occupied instead of closing through the actor. Entry/exit interpolate; the common model root reclines body and head together along the bed's long axis, with resting eyes.
- Lid geometry uses the original PRS node's slanted hinge, monotonic 0/25/50/75-degree poses, and one fixed rest-space fitting transform. Hull vertices remain unchanged across all frames.
- Coffin collision follows the actual closed hull footprint. Mounted cleanup checks floor support and full actor clearance, then tries adjacent exits, the collision top, and a bounded nearby search. If the local volume is completely sealed, it cancels play but retains recovery ownership across reload, suspends AI/gravity, and retries until a validated exit becomes available; original AI/gravity are restored only after relocation. A safe external rescue position is accepted even far from the original toy. Wall-suffocation damage is suppressed only during pending recovery; ordinary invulnerability is not enabled. Coffin choreography cancels before using an obstructed exit.
- A bounded approach reservation prevents two Chao claiming a keyboard while navigation is stopped for final precision movement. Ball, TV and other toys retain their existing choreography; supported mounted-exit checks also benefit horse/lifesaver cleanup.

## Completed here

Run `scripts/test_toy_interactions_02518.sh`:

- Pure Java `ToyInteractionRulesTest`: **610 assertions passed** using the installed Java 21 compiler module.
- `scripts/test_coffin_geometry_02518.py`: **4 tests passed**, including original PRS hinge decoding, rigid lid, unchanged hull, monotonic opening and generated/shipped OBJ agreement.
- Python compilation of the touched generator, validator and geometry test passed.
- Compiler parsing of six touched/new Java files passed without syntax errors. Parsing is **not** Minecraft/NeoForge type checking.
- An independent source review checked contact envelopes against the OBJ bounds and caught the conflicting coffin look target, which is corrected.

The broad legacy `validate_sa2_assets_02515.py` passes its toy section but fails later at the unrelated `Missing garment bevel` check. No garment assets were changed by this work. This is not an aggregate validation pass.

## Native checks added but not executed here

`ToyInteractionGameTests02518` adds 18 actual server GameTests:

- Piano and organ, all four facings, routing from behind to precise collision-clear contact and completing play.
- Blocked keyboard front cannot fall back to the rear.
- Displaced keyboard actor cancels without reward.
- Two competing precision approaches retain exclusive ownership.
- Coffin opening count, held-open rest, exit and completion/gravity restoration.
- Coffin reload, destruction, and obstructed-exit cleanup without completion rewards.
- All immediate exits and the overhead space blocked: collision-free supported wider fallback.
- A fully sealed recovery search volume with a non-invulnerable actor and the coffin replaced by solid stone: preserve mount state and health across reload, prohibit new play, and recover after a safe cell opens without duplicating rewards.
- External rescue more than six blocks from the original toy: restore temporary AI/gravity state at the new supported location.

The rules test is also registered in the normal `MechanicsTestSuite`.

## Blocker and required next verification

No Gradle distribution or dependency cache is installed. With writable `GRADLE_USER_HOME=/workspace/shared/gradle-toy-home`, the wrapper attempts its official Gradle 9.2.1 distribution download and fails with `java.net.SocketException: Network is unreachable`. The initial default Gradle home was also unwritable. No dependency workaround or network restriction bypass was attempted.

Consequently there is **no full compile, new JAR, native server result, or native visual pass** for these changes. In an authorized environment with dependencies available:

1. Run the normal Gradle build/core suite and the new plus existing toy GameTests.
2. Capture actual child/adult piano and organ performances at all orientations, checking hands at keys, body facing, distance and obstacle routing.
3. Capture a whole coffin session, including entry, still nap, exit, closing, and interrupted/reloaded cases. Confirm root lean and `.18` long-axis offset visually; mathematical and server assertions cannot establish the final rendered fit, especially for different Chao anatomy/accessories.
4. Re-run the aggregate asset pipeline with its current release-compatible garment validation, then package and verify a new artifact only after those checks pass.
