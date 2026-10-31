# SDD ledger — plan: docs/superpowers/plans/2026-10-31-performance-update.md

Base:8d33b77b0bb909f9be38812eebdc532c57dc01de. User explicitly authorizes autonomous update, research, build and delivery. Native execution; independent requesting-code-review baseline/review agent review_cpu_update is read-only.

Ruling: dedicated feature branch in existing workspace, preserve large SDK/native cache and local signing helper rather than duplicate workspace. No concurrent writers; cost is shared ignored build cache, so do not run overlapping native builds.
Ruling: preserve32 synthesis iterations and complete references; shorten only preview target. No measured quality basis for lowering iterations or cropping16s reference.
Ruling: no KleidiAI dependency: installed Q4_K/Q6_K tensors unsupported by that integration. Use existing kernels in isolated second JNI variant, flags armv8-a+dotprod+i8mm, guarded by actual auxvec capabilities and primary ARM64. Upstream dynamic DL migration larger than needed; cost is an extra native .so.
Preflight Task1→Task2: cloneInput preserves full transcript and only changes preview text; progress leaves request/profile schema stable.
Preflight Task2→Task3: generic and optimized JNI must expose distinct classes but identical numeric progress protocol; both must clean scoped callbacks before Control destruction.

Task1: initial missing API compilation failure logged /tmp/voclone-update-preview-red-initial.log. Existing payload extracted without changing behavior; characterization preservation check passed in /tmp/voclone-update-preview-characterization.log. Next add regression long-preview tests, observe real assertion failure, then bound target.
Native audit: reviewer confirmed Release/O3, generic noI8MM, KleidiAI Q4_0/Q8_0 only, FA CPU no-op, four-thread default, static symbol isolation viable. Actual NDK27 preprocess shows proposed flag adds only dotprod+i8mm beyond baseline, no fp16/SVE/crypto. Need runtime math selftest (Q4_K/Q6_K even/odd/singlecol; scalar reference with dequantizedQ8_K RHS), final exports inspection and actual device benchmarking still missing.

Task1 complete: /tmp/voclone-update-preview-red.log shows5 tests,4 genuine assertion failures on long target/Unicode/blank cases. /tmp/voclone-update-preview-green.log whole JVM suite passed after bounded preview and VM integration; full transcript unchanged, UI long-reference advice and reference-duration label added.
