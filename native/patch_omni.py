#!/usr/bin/env python3
"""Reviewed VoClone patch against the exact pinned OmniVoice sources (public ABI unchanged)."""
from pathlib import Path

def patch(root: Path):
    mask=root/'src/maskgit-tts.h'
    text=mask.read_text()
    if '// VoClone cooperative cancellation' not in text:
        needle='struct MaskgitConfig {'
        assert text.count(needle)==1
        text=text.replace(needle,needle+'\n    // VoClone cooperative cancellation, internal config only; no public ABI change.\n    bool (*cancel)(void*) = nullptr;\n    void* cancel_data = nullptr;')
        text=text.replace('    MaskgitBatchedCtx batched_ctx;','    if (cfg.cancel && cfg.cancel(cfg.cancel_data)) return {};\n    MaskgitBatchedCtx batched_ctx;')
        needle='    for (int step = 0; step < cfg.num_step; step++) {'
        assert text.count(needle)==1
        text=text.replace(needle,needle+'\n        if (cfg.cancel && cfg.cancel(cfg.cancel_data)) return {};')
        needle='        const std::chrono::steady_clock::time_point fwd_t1 = std::chrono::steady_clock::now();'
        assert text.count(needle)==1
        text=text.replace(needle,'        if (cfg.cancel && cfg.cancel(cfg.cancel_data)) return {};\n'+needle)
        text=text.replace('    return tokens;','    if (cfg.cancel && cfg.cancel(cfg.cancel_data)) return {};\n    return tokens;')
        mask.write_text(text)
    pipeline=root/'src/pipeline-tts.cpp'
    text=pipeline.read_text()
    if '// VoClone CPU abort lease' not in text:
        text=text.replace('#include "ggml-backend.h"','#include "ggml-backend.h"\n#include "ggml-cpu.h"')
        needle='    tts_cancel cc = { params->cancel, params->cancel_user_data, false };'
        assert text.count(needle)==1
        text=text.replace(needle,needle+'''
    // VoClone CPU abort lease. Always remove the callback before stack cc dies.
    mg_cfg.cancel = [](void* data) { return tts_should_cancel(static_cast<tts_cancel*>(data)); };
    mg_cfg.cancel_data = &cc;
    struct CpuAbortLease {
        ggml_backend_t backend;
        CpuAbortLease(ggml_backend_t b, tts_cancel* c): backend(b) {
            if (backend) ggml_backend_cpu_set_abort_callback(backend, [](void* p) {
                return tts_should_cancel(static_cast<tts_cancel*>(p));
            }, c);
        }
        ~CpuAbortLease() { if (backend) ggml_backend_cpu_set_abort_callback(backend, nullptr, nullptr); }
    } abort_lease(pt->bp.cpu_backend, &cc);
''')
        needle='    std::vector<float> audio = pipeline_codec_decode(pc, tokens.data(), K, T);'
        assert text.count(needle)==1
        text=text.replace(needle,'    if (mg_cfg.cancel && mg_cfg.cancel(mg_cfg.cancel_data)) return {};\n'+needle)
        needle='    if (has_raw && re.has_ref && re.ref_codes.empty()) {'
        assert text.count(needle)==1
        text=text.replace(needle,'    if (tts_should_cancel(&cc)) return OV_STATUS_CANCELLED;\n'+needle)
        pipeline.write_text(text)
    # Numeric stage/step events only; no text/audio is added to telemetry.
    text=mask.read_text()
    if '// VoClone numeric progress' not in text:
        text=text.replace('#include "debug.h"','#include "debug.h"\n#include "ov-error.h"')
        needle='    for (int step = 0; step < cfg.num_step; step++) {'
        assert text.count(needle)==1
        text=text.replace(needle,needle+'\n        // VoClone numeric progress; done counts completed iterations.\n        ov_log(OV_LOG_INFO, "[VoClone-Progress] 3 %d %d", step, cfg.num_step);')
        needle='        fprintf(stderr, "[MaskGIT-Step]'
        assert text.count(needle)==1
        text=text.replace(needle,'        ov_log(OV_LOG_INFO, "[VoClone-Progress] 3 %d %d", step + 1, cfg.num_step);\n'+needle)
        mask.write_text(text)
    text=pipeline.read_text()
    if '// VoClone reference progress' not in text:
        needle='    RefEncoded re ='
        assert text.count(needle)==1
        text=text.replace(needle,'    // VoClone reference progress\n    ov_log(OV_LOG_INFO, "[VoClone-Progress] 2 0 1");\n'+needle)
        needle='    // Resolve the reference triple fed to the synthesis helpers.'
        assert text.count(needle)==1
        text=text.replace(needle,'    ov_log(OV_LOG_INFO, "[VoClone-Progress] 2 1 1");\n\n'+needle)
        needle='    std::vector<float> audio = pipeline_codec_decode(pc, tokens.data(), K, T);'
        assert text.count(needle)==1
        text=text.replace(needle,'    ov_log(OV_LOG_INFO, "[VoClone-Progress] 4 0 1");\n'+needle+'\n    if (!audio.empty()) ov_log(OV_LOG_INFO, "[VoClone-Progress] 4 1 1");')
        pipeline.write_text(text)

if __name__=='__main__':
    patch(Path(__file__).resolve().parent/'build/sources/omnivoice')
