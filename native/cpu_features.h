#pragma once
namespace vc {
inline bool supports_i8mm(unsigned long hwcap,unsigned long hwcap2) {
    return (hwcap&(1UL<<20))!=0 && (hwcap2&(1UL<<13))!=0;
}
}
