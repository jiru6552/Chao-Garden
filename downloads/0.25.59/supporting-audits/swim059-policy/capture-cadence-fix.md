# Developer-only capture scheduling correction

Actual third-cohort case55 reached its native240-independent original140-tick gameplay endpoint but lacked the required third LEFT camera screenshot. Front captured12/24/36, left60/72, back96/108/120/132. Tick48 landed inside camera-settling frames and no eligible render frame landed on exact tick84. The former elapsed%12==0 gate silently discarded the next eligible frame.

Root approved exactly two changes to the JAR-excluded developer helper NeutralSwimVisualSmoke059:

1. Actual Accessories carry scenes run240 instead of140 normal20TPS ticks, providing more real motion in each of the three camera windows.
2. Capture when elapsed-lastMotionCapture>=12 instead of requiring elapsed%12==0, so a delayed render frame still captures the current real state rather than missing that period entirely.

The elapsed>=6 and viewFrames>=3 guards remain. Actual actor/world rendering requirements, native20TPS/walltime verification, SHA capture proof and minimum3 screenshots per motion angle remain unchanged. No missing angle is filled with a static duplicate or lower quota. Catalog remains65 with unchanged identities/names. Production sources/JAR are unchanged; only developer-only helper compilation and a complete new65-case capture cohort are required.
