#include "../mp3/include/Mp3Encoder.h"
#include <assert.h>
#include <stdio.h>
#include <stdint.h>
int main(int argc,char **argv) {
    assert(argc==2);
    VSMp3 *encoder=vs_mp3_create(argv[1],24000,24000ULL*5*3600,1);
    assert(encoder && "A five-hour streaming export must not hit an arbitrary four-hour limit");
    vs_mp3_free(encoder);remove(argv[1]);
    assert(!vs_mp3_create(argv[1],24000,UINT64_MAX,1));
    puts("PASS long MP3 frame-count initialization; no audio synthesis claimed");
}
