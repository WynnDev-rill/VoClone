#include "../quant_selftest.h"
#include <cassert>
#include <iostream>
int main() {
    assert(quant_matmul_selftest());
    std::cout<<"PASS actual Q4_K/Q6_K matrix kernels against dequantized scalar reference (even, odd, single-column)\n";
}
