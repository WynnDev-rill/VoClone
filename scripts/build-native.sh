#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../native" && pwd)"
python3 "$root/fetch.py"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/workspace/android-sdk}}"
ndk="$sdk/ndk/27.1.12297006"
cmake="$sdk/cmake/3.22.1/bin/cmake"
ninja="$sdk/cmake/3.22.1/bin/ninja"
export GIT_CEILING_DIRECTORIES="$root/build/sources"
for abi in ${VOCLONE_ABIS:-arm64-v8a}; do
    common=(-G Ninja -DCMAKE_MAKE_PROGRAM="$ninja" -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-29 -DANDROID_STL=c++_shared -DCMAKE_BUILD_TYPE=Release -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DBUILD_SHARED_LIBS=OFF -DGGML_NATIVE=OFF -DGGML_OPENMP=OFF -DGGML_LLAMAFILE=OFF -DGGML_CPU_KLEIDIAI=OFF -DGGML_METAL=OFF -DGGML_BLAS=OFF -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384)
    "$cmake" -S "$root/build/sources/omnivoice" -B "$root/build/omni-$abi" "${common[@]}" -DOMNIVOICE_SHARED=OFF
    "$cmake" --build "$root/build/omni-$abi" --target omnivoice-core -j 2
    "$cmake" -S "$root/build/sources/whisper" -B "$root/build/whisper-$abi" "${common[@]}" -DWHISPER_BUILD_TESTS=OFF -DWHISPER_BUILD_EXAMPLES=OFF -DWHISPER_BUILD_SERVER=OFF
    "$cmake" --build "$root/build/whisper-$abi" --target whisper -j 2
    "$cmake" -S "$root" -B "$root/build/jni-$abi" "${common[@]}" -DRUNTIME_ABI="$abi"
    "$cmake" --build "$root/build/jni-$abi" -j 2
    mkdir -p "$root/build/jniLibs/$abi"
    cp "$root/build/jni-$abi/"libvc_*.so "$root/build/jniLibs/$abi/"
    toolchains=("$ndk/toolchains/llvm/prebuilt/"*)
    triple=aarch64-linux-android
    if [[ "$abi" == x86_64 ]]; then triple=x86_64-linux-android; fi
    cp "${toolchains[0]}/sysroot/usr/lib/$triple/libc++_shared.so" "$root/build/jniLibs/$abi/"
done
