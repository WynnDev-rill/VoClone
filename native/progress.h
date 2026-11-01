#pragma once
#include <charconv>
#include <cstring>
#include <string_view>
namespace vc {
struct Progress {int stage=0,done=0,total=0;};
// Only our numeric protocol crosses JNI. Never forward arbitrary runtime logs.
inline bool parse_progress(const char* message,Progress& output) {
    if(!message)return false;
    std::string_view input(message),prefix="[VoClone-Progress] ";
    if(input.substr(0,prefix.size())!=prefix)return false;
    input.remove_prefix(prefix.size());
    int fields[3]{};
    for(int i=0;i<3;++i) {
        auto parsed=std::from_chars(input.data(),input.data()+input.size(),fields[i]);
        if(parsed.ec!=std::errc{})return false;
        const auto used=static_cast<size_t>(parsed.ptr-input.data());
        input.remove_prefix(used);
        if(i<2) { if(input.empty()||input.front()!=' ')return false;input.remove_prefix(1); }
    }
    if(!input.empty()||fields[0]<2||fields[0]>4||fields[1]<0||fields[2]<1||fields[2]>10000||fields[1]>fields[2])return false;
    output={fields[0],fields[1],fields[2]};return true;
}
}
