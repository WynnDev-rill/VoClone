#include "../cpu_features.h"
#include <cassert>
#include <iostream>
int main() {
    assert(vc::supports_i8mm(1UL<<20,1UL<<13));
    assert(!vc::supports_i8mm(0,1UL<<13));
    assert(!vc::supports_i8mm(1UL<<20,0));
    assert(!vc::supports_i8mm(~(1UL<<20),~0UL));
    assert(vc::supports_i8mm(~0UL,~0UL));
    std::cout<<"PASS ARM feature gate\n";
}
