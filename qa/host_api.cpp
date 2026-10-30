// Real OmniVoice C API smoke/cancellation test. Uses the exact same patched core as Android JNI.
#include "omnivoice.h"
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <iostream>
#include <iterator>
#include <memory>
#include <stdexcept>
#include <vector>
#include <string>
static std::string read(const std::string& path) {std::ifstream f(path,std::ios::binary);return {std::istreambuf_iterator<char>(f),{}};}
struct Stop {std::chrono::steady_clock::time_point start;bool triggered=false;};
static bool cancelled(void* data) {auto s=static_cast<Stop*>(data);s->triggered=std::chrono::steady_clock::now()-s->start>std::chrono::seconds(2);return s->triggered;}
static void wav(const std::string& path,const ov_audio& a) {
    if(a.n_samples<2400||a.sample_rate!=24000||a.channels!=1)throw std::runtime_error("invalid output shape");
    std::ofstream f(path,std::ios::binary);auto u16=[&](uint16_t v){f.put(v&255);f.put(v>>8);};auto u32=[&](uint32_t v){u16(v&65535);u16(v>>16);};
    f.write("RIFF",4);u32(36+a.n_samples*2);f.write("WAVEfmt ",8);u32(16);u16(1);u16(1);u32(24000);u32(48000);u16(2);u16(16);f.write("data",4);u32(a.n_samples*2);
    for(int i=0;i<a.n_samples;i++) {float v=a.samples[i];if(!std::isfinite(v))throw std::runtime_error("nonfinite output");v=std::max(-1.f,std::min(1.f,v));u16(static_cast<int16_t>(std::lround(v*32767)));}
    if(!f)throw std::runtime_error("output write failed");
}
int main(int argc,char** argv) {
    if(argc!=6) {std::cerr<<"model codec reference.f32 reference.txt output-dir\n";return 2;}
    ov_init_params init{};ov_init_default_params(&init);init.model_path=argv[1];init.codec_path=argv[2];init.use_fa=false;
    std::unique_ptr<ov_context,decltype(&ov_free)> ctx(ov_init(&init),ov_free);if(!ctx)return 3;
    auto bytes=read(argv[3]);if(bytes.size()%4)return 4;
    std::vector<float> reference(bytes.size()/4);std::memcpy(reference.data(),bytes.data(),bytes.size());
    auto words=read(argv[4]);
    ov_tts_params p{};ov_tts_default_params(&p);p.ref_audio_24k=reference.data();p.ref_n_samples=reference.size();p.ref_text=words.c_str();p.text="Hello, friend.";p.lang="en";p.mg_seed=42;
    Stop stop{std::chrono::steady_clock::now()};p.cancel=cancelled;p.cancel_user_data=&stop;
    ov_audio a{};auto status=ov_synthesize(ctx.get(),&p,&a);auto elapsed=std::chrono::duration<double>(std::chrono::steady_clock::now()-stop.start).count();
    std::cout<<"CANCEL status="<<status<<" triggered="<<stop.triggered<<" seconds="<<elapsed<<" samples="<<a.n_samples<<std::endl;
    ov_audio_free(&a);if(!stop.triggered||status!=OV_STATUS_CANCELLED||elapsed>60)return 5;
    p.cancel=nullptr;p.cancel_user_data=nullptr;
    const char* languages[]={"en","id","ja"};const char* texts[]={"Hello, friend.","Halo, dunia.","こんにちは。"};
    for(int i=0;i<3;i++) {
        p.lang=languages[i];p.text=texts[i];auto started=std::chrono::steady_clock::now();
        status=ov_synthesize(ctx.get(),&p,&a);
        if(status!=OV_STATUS_OK){std::cerr<<ov_last_error()<<std::endl;return 6;}
        wav(std::string(argv[5])+"/api-"+languages[i]+".wav",a);
        std::cout<<"SYNTH language="<<languages[i]<<" seconds="<<std::chrono::duration<double>(std::chrono::steady_clock::now()-started).count()<<" frames="<<a.n_samples<<std::endl;
        ov_audio_free(&a);
    }
}
