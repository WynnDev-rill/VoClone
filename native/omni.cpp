// Adapted from VoiceStudio's AGPL-3.0 Android bridge; VoClone allocation/cancellation guards.
#include "control.h"
#include "omnivoice.h"
#include "progress.h"
#include "quant_selftest.h"
#include "cpu_features.h"
#if defined(__aarch64__)
#include <sys/auxv.h>
#include <asm/hwcap.h>
static_assert(HWCAP_ASIMDDP==(1UL<<20) && HWCAP2_I8MM==(1UL<<13));
#endif
#include <memory>
#include <vector>
#include <cmath>
#include <new>
#ifdef VC_I8MM_VARIANT
#define VC_JNI(name) Java_id_wynn_voclone_FastNativeBridge_##name
#else
#define VC_JNI(name) Java_id_wynn_voclone_NativeBridge_##name
extern "C" JNIEXPORT jboolean JNICALL Java_id_wynn_voclone_NativeBridge_hasI8mm(JNIEnv*,jobject) {
#if defined(__aarch64__)
    return vc::supports_i8mm(getauxval(AT_HWCAP),getauxval(AT_HWCAP2)) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}
#endif
extern "C" JNIEXPORT jboolean JNICALL VC_JNI(mathSelfTest)(JNIEnv*,jobject) {
    try { return quant_matmul_selftest() ? JNI_TRUE : JNI_FALSE; } catch(...) { return JNI_FALSE; }
}
extern "C" JNIEXPORT jint JNICALL VC_JNI(omniAbi)(JNIEnv*,jobject) { return OV_ABI_VERSION; }
extern "C" JNIEXPORT jfloatArray JNICALL VC_JNI(cloneAudio)(JNIEnv* e,jobject,jstring model,jstring codec,jstring text,jstring refText,jfloatArray reference,jstring language,jobject control) {
    try {
        if(!model||!codec||!text||!refText||!reference||!language||!control){fail(e);return nullptr;}
        Utf8 m(e,model),c(e,codec),t(e,text),r(e,refText),l(e,language);
        if(e->ExceptionCheck())return nullptr;
        Control ctl(e,control);if(e->ExceptionCheck()||ctl.failed)return nullptr;
        const auto count=e->GetArrayLength(reference);
        if(count<72000||count>720000){fail(e);return nullptr;}
        std::vector<float> ref(count);e->GetFloatArrayRegion(reference,0,count,ref.data());
        if(e->ExceptionCheck())return nullptr;
        for(float f:ref)if(!std::isfinite(f)){fail(e);return nullptr;}
        struct LogLease {
            explicit LogLease(Control* control) {
                ov_log_set([](ov_log_level,const char* message,void* user) {
                    vc::Progress event;
                    if(vc::parse_progress(message,event))static_cast<Control*>(user)->status(event.stage,event.done,event.total);
                },control);
            }
            ~LogLease() { ov_log_set([](ov_log_level,const char*,void*){},nullptr); }
        } logging(&ctl);
        ov_init_params init{};ov_init_default_params(&init);init.model_path=m.chars;init.codec_path=c.chars;init.use_fa=false;
        if(Control::cancelled(&ctl))return nullptr;
        ctl.status(1,0,0);
        std::unique_ptr<ov_context,decltype(&ov_free)> context(ov_init(&init),ov_free);
        if(!context){fail(e);return nullptr;}
        ov_tts_params params{};ov_tts_default_params(&params);params.text=t.chars;params.ref_text=r.chars;params.lang=(l.storage=="none" ? nullptr : l.chars);
        params.ref_audio_24k=ref.data();params.ref_n_samples=count;params.cancel=Control::cancelled;params.cancel_user_data=&ctl;
        struct AudioLease { ov_audio value{}; ~AudioLease(){ov_audio_free(&value);} } audio;
        const auto status=ov_synthesize(context.get(),&params,&audio.value);
        if(Control::cancelled(&ctl))return nullptr;
        if(status!=OV_STATUS_OK || !audio.value.samples || audio.value.n_samples<2400 || audio.value.n_samples>24000*180 || audio.value.sample_rate!=24000 || audio.value.channels!=1){fail(e);return nullptr;}
        auto result=e->NewFloatArray(audio.value.n_samples);
        if(!result||e->ExceptionCheck())return nullptr;
        e->SetFloatArrayRegion(result,0,audio.value.n_samples,audio.value.samples);
        if(e->ExceptionCheck())return nullptr;
        if(ctl.failed){fail(e);return nullptr;}return result;
    } catch(const std::bad_alloc&) { oom(e);return nullptr; }
    catch(...) { fail(e);return nullptr; }
}
