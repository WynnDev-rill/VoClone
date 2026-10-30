# VoClone design
Source authority: user-provided reference image, PRODUCT.md and design-system/voclone/MASTER.md. User constraints override generated web-layout recommendations.

Native Material 3 Operate surface. One bounded-width scrolling column; no bottom dock. Header contains supplied icon, VoClone wordmark, small settings action. Hero: “Clone Your Voice”. Audio reference is the only prominent tonal container, with true waveform/player, import/record controls. Primary blue Clone Voice action. Saved voices use simple rows with initials, use and overflow actions. Text editor and Generate button below. Settings contains models, licences, theme, updater and source.

Light: primary #1764DF, onPrimary #FFFFFF, background #F8FAFF, surface #FFFFFF, onSurface #101B35, secondary text #53617A, outline #C4CFE1. Dark: primary #A8C7FF, onPrimary #003062, background #0D1422, surface #172033, text #E6EDFA, secondary #BAC7DD. Plus Jakarta Sans local font (OFL). Body 16sp; title 22sp; headline 34sp. Spacing 8/12/16/24/32dp. Shapes 12/16dp. Width cap 640dp. Native ripples and system-respecting animated state changes.

Never fabricate waveforms, profiles or percentages. Empty waveform is an idle straight line. Inference phases use indeterminate progress except native callbacks; downloads show verified byte counts. Errors include a recovery action. Re-recording/importing cannot race cloning. Large fonts can wrap all text; compact actions switch to vertical layout when necessary.

Verification: Android emulator screenshots in qa/screenshots, dark mode, 320dp and 1.3 font scale. Hardware testing required for performance and subjective voice similarity.
