// Executes the pinned, patched production core. No synthetic fixtures stand in for AI output.
#include "omnivoice.h"
#include "../native/progress.h"
#include <chrono>
#include <cmath>
#include <fstream>
#include <iostream>
#include <iterator>
#include <vector>
#include <cstring>
#include <memory>
struct Events { int reference=0,steps=0,decoded=0; };
static void log(enum ov_log_level,const char* text,void* opaque) {
    auto &e=*static_cast<Events*>(opaque);vc::Progress p;
    if(vc::parse_progress(text,p)) {
        if(p.stage==2 && p.done==p.total)e.reference++;
        if(p.stage==3 && p.done>e.steps)e.steps=p.done;
        if(p.stage==4 && p.done==p.total)e.decoded++;
        std::cout<<"PROGRESS "<<p.stage<<" "<<p.done<<" "<<p.total<<std::endl;
    }
}
static bool cancel(void* value) { return std::chrono::steady_clock::now()-*static_cast<std::chrono::steady_clock::time_point*>(value)>std::chrono::seconds(2); }
int main(int argc,char** argv) {
    if(argc!=6)return 2;
    Events events;ov_log_set(log,&events);
    ov_init_params init{};ov_init_default_params(&init);init.model_path=argv[1];init.codec_path=argv[2];init.use_fa=false;
    auto start=std::chrono::steady_clock::now();
    std::unique_ptr<ov_context,decltype(&ov_free)> ctx(ov_init(&init),ov_free);if(!ctx)return 3;
    std::ifstream data(argv[3],std::ios::binary),txt(argv[4]);
    std::vector<char> bytes{std::istreambuf_iterator<char>(data),{}};if(bytes.empty()||bytes.size()%4)return 4;
    std::vector<float> reference(bytes.size()/4);std::memcpy(reference.data(),bytes.data(),bytes.size());
    std::string words{std::istreambuf_iterator<char>(txt),{}};
    ov_tts_params p{};ov_tts_default_params(&p);p.ref_audio_24k=reference.data();p.ref_n_samples=reference.size();p.ref_text=words.c_str();p.text="Hello.";p.lang="en";p.mg_seed=42;
    auto cancelStart=std::chrono::steady_clock::now();p.cancel=cancel;p.cancel_user_data=&cancelStart;
    ov_audio audio{};auto status=ov_synthesize(ctx.get(),&p,&audio);
    double cancelSeconds=std::chrono::duration<double>(std::chrono::steady_clock::now()-cancelStart).count();
    ov_audio_free(&audio);if(status!=OV_STATUS_CANCELLED||cancelSeconds>60)return 5;
    std::cout<<"CANCEL_PASS seconds="<<cancelSeconds<<std::endl;
    p.cancel=nullptr;p.cancel_user_data=nullptr;events={};
    status=ov_synthesize(ctx.get(),&p,&audio);
    if(status!=OV_STATUS_OK||audio.channels!=1||audio.sample_rate!=24000||audio.n_samples<2400)return 6;
    double energy=0;for(int i=0;i<audio.n_samples;i++) { if(!std::isfinite(audio.samples[i]))return 7;energy+=audio.samples[i]*audio.samples[i]; }
    if(std::sqrt(energy/audio.n_samples)<.0001||events.reference<1||events.steps!=32||events.decoded<1)return 8;
    std::ofstream out(argv[5],std::ios::binary);
    auto u16=[&](uint16_t n){out.put(n&255);out.put(n>>8);};auto u32=[&](uint32_t n){u16(n&65535);u16(n>>16);};
    out.write("RIFF",4);u32(36+audio.n_samples*2);out.write("WAVEfmt ",8);u32(16);u16(1);u16(1);u32(24000);u32(48000);u16(2);u16(16);out.write("data",4);u32(audio.n_samples*2);
    for(int i=0;i<audio.n_samples;i++)u16(static_cast<int16_t>(std::lround(std::fmax(-1.f,std::fmin(1.f,audio.samples[i]))*32767)));
    out.close();if(!out)return 9;
    std::cout<<"PASS real AI + numeric progress ref="<<events.reference<<" iterations="<<events.steps<<" decode="<<events.decoded<<" frames="<<audio.n_samples<<" seconds_including_load_cancel="<<std::chrono::duration<double>(std::chrono::steady_clock::now()-start).count()<<std::endl;
    ov_audio_free(&audio);ctx.reset();ov_log_set(nullptr,nullptr);
}
