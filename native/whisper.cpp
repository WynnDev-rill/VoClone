// VoClone Whisper adapter. whisper.cpp and weights are independently MIT licensed.
#include "control.h"
#include "whisper.h"
#include <memory>
#include <vector>
extern "C" JNIEXPORT jbyteArray JNICALL Java_id_wynn_voclone_NativeBridge_transcribe(JNIEnv* e,jobject,jstring model,jfloatArray samples,jobject control) {
    try {
    if(!model||!samples||!control){fail(e);return nullptr;}
    Utf8 path(e,model);if(e->ExceptionCheck())return nullptr;
    Control ctl(e,control);if(e->ExceptionCheck()||ctl.failed)return nullptr;
    if(e->GetArrayLength(samples)<48000 || e->GetArrayLength(samples)>480000){fail(e);return nullptr;}
    whisper_log_set([](enum ggml_log_level,const char*,void*){},nullptr);
    auto init=whisper_context_default_params();init.use_gpu=false;
    std::unique_ptr<whisper_context,decltype(&whisper_free)> ctx(whisper_init_from_file_with_params(path.chars,init),whisper_free);
    if(!ctx){fail(e);return nullptr;}
    std::vector<float> audio(e->GetArrayLength(samples));e->GetFloatArrayRegion(samples,0,audio.size(),audio.data());
    auto params=whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads=2;params.translate=false;params.language="auto";
    params.print_realtime=false;params.print_progress=false;params.print_timestamps=false;params.print_special=false;
    params.no_context=true;params.single_segment=true;
    params.abort_callback=Control::cancelled;params.abort_callback_user_data=&ctl;
    params.progress_callback=[](whisper_context*,whisper_state*,int p,void* data){static_cast<Control*>(data)->progress(p/100.f);};
    params.progress_callback_user_data=&ctl;
    if(Control::cancelled(&ctl))return nullptr;
    if(whisper_full(ctx.get(),params,audio.data(),audio.size())!=0){if(!Control::cancelled(&ctl))fail(e);return nullptr;}
    if(Control::cancelled(&ctl))return nullptr;
    std::string text;
    for(int i=0;i<whisper_full_n_segments(ctx.get());i++)text+=whisper_full_get_segment_text(ctx.get(),i);
    if(text.size()>24000){fail(e);return nullptr;}
    auto result=e->NewByteArray(text.size());if(!result||e->ExceptionCheck())return nullptr;
    e->SetByteArrayRegion(result,0,text.size(),reinterpret_cast<const jbyte*>(text.data()));
    return result;
    } catch(const std::bad_alloc&) { oom(e);return nullptr; }
    catch(...) { fail(e);return nullptr; }
}
