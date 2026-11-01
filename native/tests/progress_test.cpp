#include "../progress.h"
#include <cassert>
#include <iostream>
int main() {
    vc::Progress p{};
    assert(vc::parse_progress("[VoClone-Progress] 3 4 32",p));
    assert(p.stage==3 && p.done==4 && p.total==32);
    assert(vc::parse_progress("[VoClone-Progress] 2 0 1",p));
    assert(vc::parse_progress("[VoClone-Progress] 4 1 1",p));
    for(const char* input:{"reference text", "[TTS] user text [VoClone-Progress] 3 4 32", "[VoClone-Progress] 3 4 32 secret", "[VoClone-Progress] 9 1 1", "[VoClone-Progress] 3 -1 32", "[VoClone-Progress] 3 33 32", "[VoClone-Progress] 3 1 0", "[VoClone-Progress] 3 99999999999999999999 32", "[VoClone-Progress] 3 1 32\n"})assert(!vc::parse_progress(input,p));
    assert(!vc::parse_progress(nullptr,p));
    std::cout<<"PASS numeric native progress parsing and privacy boundary\n";
}
