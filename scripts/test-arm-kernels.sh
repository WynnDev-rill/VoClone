#!/usr/bin/env bash
# Ubuntu: apt install g++-aarch64-linux-gnu qemu-user cmake ninja-build
set -euo pipefail
cd "$(dirname "$0")/.."
python3 native/fetch.py
for variant in generic i8mm; do
  arch=armv8-a
  if [[ "$variant" == i8mm ]]; then arch=armv8-a+dotprod+i8mm; fi
  build="native/build/quant-arm-$variant"
  cmake -G Ninja -S native/build/sources/ggml -B "$build" \
    -DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_PROCESSOR=aarch64 \
    -DCMAKE_C_COMPILER=aarch64-linux-gnu-gcc -DCMAKE_CXX_COMPILER=aarch64-linux-gnu-g++ \
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF -DGGML_NATIVE=OFF -DGGML_CPU_ARM_ARCH="$arch" \
    -DGGML_OPENMP=OFF -DGGML_CPU_KLEIDIAI=OFF -DGGML_BLAS=OFF -DGGML_BUILD_TESTS=OFF -DGGML_BUILD_EXAMPLES=OFF
  cmake --build "$build" --target ggml -j 2
  mapfile -t libs < <(find "$build" -name '*.a')
  aarch64-linux-gnu-g++ -std=c++17 -O2 native/tests/quant_test.cpp \
    -Inative/build/sources/ggml/include -Inative/build/sources/ggml/src \
    -Wl,--start-group "${libs[@]}" -Wl,--end-group -ldl -lm -lpthread -o "$build/quant-test"
  cpu=max
  if [[ "$variant" == generic ]]; then cpu=cortex-a53; fi
  qemu-aarch64 -L /usr/aarch64-linux-gnu -cpu "$cpu" "$build/quant-test"
done
