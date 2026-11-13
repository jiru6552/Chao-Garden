# Final legacy material audit — 061

Reviewed only `/workspace/.setup/fly061-client-final` evidence. All 30 screenshots for cases41–45 were checked against their literal `FLY061_CAPTURE` SHA256 lines and personally inspected through five labeled six-angle sheets. Original files are retained; sheets apply only the same crop and uniform50% scale. The case41 above original was also opened directly.

The final JAR SHA256 is `2618d3ed5326621d782e3a396fe68edb25b03c8f657206407641c4d840c02060`, matching the frozen candidate. All25 legacy material/region class entries (including nested classes) are byte-identical between the .60 and .61 JARs.

Each actual `FLY061_RENDER_MODEL` record remains on its established prefix: `hero_run055`, `hero_normal057`, `neutral056`, `neutral_swim059`, or `dark_normal060`. None enters `neutral_fly061`.

- Case41: Expected Hero/Run ivory skull/body, turquoise/blue cuffs and feet, yellow chest and hollow yellow halo. Eyes/white borders intact; no new Fly swatch or wing fan appears.
- Case42: Expected Hero/Normal Power+Fly ivory skull/body, violet cuffs, gold wing details/chest and pink hollow halo. Original eyes and alpha silhouettes remain intact across all angles.
- Case43: Expected Neutral/Normal Power+Swim cyan skull and green body/limbs with yellow orb. Original face/iris pixels visibly intact. No Fly palette route.
- Case44: Expected Neutral/Swim Run+Swim green skull with cyan lower face, cyan forehead V, yellow limbs/feet and pale cyan orb. Front/side/below face and wing silhouettes intact.
- Case45: Expected Dark/Normal Run+Fly dark plum body, violet/lavender cuffs/feet and independent peach spiked marker, with subdued olive wings. Original eyes/alpha silhouettes intact; no new Fly material route.

No material routing, original-face, orb-opacity or alpha-silhouette regression was found in these static final captures. This sub-review does not assess motion, audio or multiplayer. Exact original paths, hashes, literal log lines, sheet hashes, class-entry hashes and review method are in `audit.json`.
