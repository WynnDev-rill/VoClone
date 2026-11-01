#pragma once
#include "ggml.h"
#include "ggml-backend.h"
#include "ggml-alloc.h"
#include "ggml-cpu.h"
#include "ggml-quants.h"
#include <algorithm>
#include <cmath>
#include <memory>
#include <vector>

// Used at runtime before selecting kernels; never reads model or user audio.
inline bool quant_matmul_case(ggml_type type,int k,int rows,int columns) {
    std::unique_ptr<ggml_context,decltype(&ggml_free)> context(ggml_init({2*1024*1024,nullptr,true}),ggml_free);
    std::unique_ptr<ggml_backend,decltype(&ggml_backend_free)> backend(ggml_backend_cpu_init(),ggml_backend_free);
    if(!context||!backend)return false;
    ggml_backend_cpu_set_n_threads(backend.get(),1);
    auto* a=ggml_new_tensor_2d(context.get(),type,k,rows);
    auto* b=ggml_new_tensor_2d(context.get(),GGML_TYPE_F32,k,columns);
    auto* result=ggml_mul_mat(context.get(),a,b);
    auto* graph=ggml_new_graph(context.get());ggml_build_forward_expand(graph,result);
    std::unique_ptr<ggml_backend_buffer,decltype(&ggml_backend_buffer_free)> buffer(ggml_backend_alloc_ctx_tensors(context.get(),backend.get()),ggml_backend_buffer_free);
    if(!buffer)return false;
    std::vector<float> weights(k*rows),rhs(k*columns),dequantized(k*rows),activation(k*columns);
    for(size_t i=0;i<weights.size();++i)weights[i]=(static_cast<int>((i*17+3)%97)-48)/17.f;
    for(size_t i=0;i<rhs.size();++i)rhs[i]=(static_cast<int>((i*13+9)%67)-33)/23.f;
    std::vector<unsigned char> packed(ggml_nbytes(a));
    if(ggml_quantize_chunk(type,weights.data(),packed.data(),0,rows,k,nullptr)!=packed.size())return false;
    if(type==GGML_TYPE_Q4_K)dequantize_row_q4_K(reinterpret_cast<const block_q4_K*>(packed.data()),dequantized.data(),k*rows);
    else dequantize_row_q6_K(reinterpret_cast<const block_q6_K*>(packed.data()),dequantized.data(),k*rows);
    std::vector<block_q8_K> quantized_rhs(k/256);
    for(int column=0;column<columns;++column) {
        quantize_row_q8_K_ref(rhs.data()+column*k,quantized_rhs.data(),k);
        dequantize_row_q8_K(quantized_rhs.data(),activation.data()+column*k,k);
    }
    ggml_backend_tensor_set(a,packed.data(),0,packed.size());
    ggml_backend_tensor_set(b,rhs.data(),0,rhs.size()*sizeof(float));
    if(ggml_backend_graph_compute(backend.get(),graph)!=GGML_STATUS_SUCCESS)return false;
    std::vector<float> actual(rows*columns);ggml_backend_tensor_get(result,actual.data(),0,actual.size()*sizeof(float));
    for(int column=0;column<columns;++column)for(int row=0;row<rows;++row) {
        double expected=0,magnitude=0;
        for(int i=0;i<k;++i) { double product=static_cast<double>(dequantized[row*k+i])*activation[column*k+i];expected+=product;magnitude+=std::abs(product); }
        const float value=actual[column*rows+row];
        if(!std::isfinite(value)||std::abs(value-expected)>0.0002+0.00001*magnitude)return false;
    }
    return true;
}
inline bool quant_matmul_selftest() {
    for(auto type:{GGML_TYPE_Q4_K,GGML_TYPE_Q6_K})for(int k:{256,512})
        for(auto shape:{std::pair<int,int>{1,1},{3,1},{4,4},{3,3}})
            if(!quant_matmul_case(type,k,shape.first,shape.second))return false;
    return true;
}
