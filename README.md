# 🚀 Yadra Qwen3 Vulkan Inference Engine

> **A high-performance Vulkan inference engine for Qwen3 on Android and Desktop.**
>
> Designed from the ground up to execute **entirely on the GPU**, using custom Vulkan compute shaders and quantized GGUF models, with zero CPU participation during inference.

---

## ✨ Highlights

- ⚡ **100% GPU Inference**
  - Every transformer operation runs as Vulkan compute shaders.
  - No CPU math during generation.

- 🧠 **Native Qwen3 Support**
  - GGUF model loading
  - Q4_K / Q6_K quantization
  - RMSNorm
  - Rotary Positional Embeddings (RoPE)
  - Grouped Query Attention (GQA)
  - SiLU-gated Feed Forward Network

- 📦 **GPU KV Cache**
  - Fully GPU-resident sliding-window KV cache.
  - Optimized for autoregressive decoding.

- 🔥 **Vulkan Graph Capture**
  - Records the decode pipeline once.
  - Compiles it into a reusable Vulkan graph.
  - Eliminates per-token command buffer overhead.

- 🎲 **GPU Sampling**
  - Greedy (Argmax)
  - Temperature sampling
  - Top-P
  - Top-K

- 🔤 **BPE Tokenizer**
  - Compatible with `tokenizer3.json`
  - Byte fallback
  - Merge rules
  - Special tokens

- 📡 **Streaming Generation**
  - Token callback interface.
  - Low-latency streaming responses.

- 📊 **Performance Metrics**
  - Prefill timing
  - Decode timing
  - Tokens/sec statistics

---

# Architecture

The engine is organized into a small number of core components that together implement the complete Qwen3 inference pipeline.

<p align="center">
  <img src="docs/architecture.svg" alt="Yadra Qwen3 Vulkan Architecture" width="900">
</p>

| Component | Responsibility |
|------------|----------------|
| `Qwen3Config` | Reads model metadata from GGUF |
| `Qwen3BPETokenizer` | BPE encoding and decoding |
| `KVCache` | GPU-resident key/value cache |
| `Qwen3Engine` | Complete inference pipeline |
| `Tensor / Vulkan Executor` | Vulkan runtime, tensor operations and graph execution |

---

# Inference Pipeline

```
Prompt
   │
   ▼
Tokenizer (CPU)
   │
   ▼
Token IDs
   │
   ▼
Embedding Lookup (GPU)
   │
   ▼
Transformer Layers × N
   │
   ├── Attention
   │      ├── RMSNorm
   │      ├── RoPE
   │      ├── KV Cache
   │      └── Output Projection
   │
   └── Feed Forward
          ├── Gate Projection
          ├── Up Projection
          ├── SiLU
          └── Down Projection
   │
   ▼
Final RMSNorm
   │
   ▼
LM Head
   │
   ▼
Logits
   │
   ▼
GPU Sampling
   │
   ▼
Next Token
   │
   └───────────────┐
                   │
                   ▼
             Decode Loop
```

---

## Generation Flow

### 1. Model Loading

The GGUF file is parsed and every tensor is uploaded to GPU-visible memory while preserving its original quantization.

Supported formats:

- Q4_K
- Q6_K

---

### 2. Tokenization

The prompt is encoded using a BPE tokenizer compatible with Qwen3.

Supported features include:

- merge rules
- byte fallback
- special tokens

Examples:

```
<|im_start|>
<|im_end|>
<think>
```

---

### 3. Prefill

The entire prompt is processed in one forward pass.

Pipeline:

```
Tokens
   ↓
Embedding
   ↓
Transformer Layers
   ↓
Final RMSNorm
   ↓
LM Head
   ↓
Logits
```

During this phase the KV Cache is populated.

---

### 4. Decode

Generation proceeds one token at a time.

The **first decode step** performs:

- Vulkan graph capture
- graph compilation
- pipeline optimization

Every following token simply executes the compiled graph.

This dramatically reduces Vulkan driver overhead, particularly on Mali GPUs.

---

### 5. GPU Sampling

Sampling occurs entirely on the GPU.

Available methods:

- Greedy
- Temperature
- Top-P
- Top-K

The selected token ID is written directly into a persistently mapped buffer.

---

### 6. Streaming

Each generated token is immediately returned through a callback.

```cpp
[](int32_t token)
{
    // Handle generated token
}
```

---

# Vulkan Backend

The engine is built on top of the custom **Yadra Vulkan Runtime**.

## Custom Compute Shaders

Specialized GLSL kernels include:

- MatMul Q4_K
- MatMul Q6_K
- RMSNorm
- RoPE
- Flash Attention
- SiLU
- Sampling

---

## Vulkan Graph Capture

```cpp
Tensor::executor().begin_graph();

// record operations

Tensor::executor().stop_capture();

graph->compile();

graph->execute();
```

Instead of recording hundreds of dispatches every token, the runtime executes a single precompiled graph.

---

## Persistent Mapped Buffers

Sampling results are read through persistently mapped buffers:

```cpp
result_buffer->map_persistent();
```

This avoids repeated GPU → CPU synchronization.

---

# Usage

## Desktop

```bash
./qwen3_chat [model.gguf] [tokenizer3.json] [max_seq_len]
```

Arguments:

| Argument | Description |
|-----------|-------------|
| model.gguf | GGUF model |
| tokenizer3.json | tokenizer configuration |
| max_seq_len | maximum sequence length |

---

## Interactive Chat

Example:

```
╔════════════════════════════════════════╗
║      Qwen3 Vulkan Chat (Yadra)         ║
╚════════════════════════════════════════╝

You:
```

Commands:

```
/think
```

Toggle reasoning mode.

```
exit
```

Exit application.

---

# Example

```
You:
What is the capital of France?

Assistant:
Paris.

You:
/think

You:
Solve 2+2

Assistant:
<think>

2 + 2 = 4

</think>

The answer is 4.
```

---

# Integration

```cpp
#include "yadra/qwen3/qwen3_engine.hpp"

int main()
{
    yadra::Qwen3Engine engine;

    engine.load(
        "qwen3-0.6b.Q4_K_M.gguf",
        "tokenizer3.json",
        2048
    );

    std::string response =
        engine.chat(
            "Why is the sky blue?",
            256,
            0.7f,
            false
        );

    auto tokens =
        engine.generate(
            prompt_ids,
            256,
            0.7f,
            0.9f,
            [](int32_t token)
            {
                // Streaming callback
            });

    auto stats = engine.last_stats();

    printf("Prefill : %.2f ms\n", stats.prefill_ms);
    printf("Decode  : %.2f ms\n", stats.decode_ms);
}
```

The exact same engine is used on Android through a JNI wrapper.

---

# Build

## Requirements

- C++17
- CMake ≥ 3.18
- Android NDK r25+
- Vulkan SDK

Dependencies:

- nlohmann/json
- Yadra Vulkan Runtime

---

## Desktop

```bash
cmake -B build -DCMAKE_BUILD_TYPE=Release

cmake --build build
```

---

## Android

```bash
cmake \
    -B build \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a

cmake --build build
```

---

# Performance Goals

Designed specifically for modern mobile GPUs.

Current optimization targets include:

- Mali G615 MC6
- Vulkan 1.3
- Graph Capture
- Quantized Matrix Multiplication
- Flash Attention
- GPU Sampling
- Zero CPU Inference

---

# Roadmap

- [x] GGUF loading
- [x] Q4_K inference
- [x] Q6_K inference
- [x] Vulkan graph capture
- [x] GPU sampling
- [x] Streaming generation
- [x] Android support
- [ ] Flash Attention v2
- [ ] Speculative decoding
- [ ] Multi-batch inference
- [ ] Multi-GPU execution
- [ ] Additional GGUF quantization formats

---

# Contributing

Contributions are welcome.

Please open an issue before making major architectural changes.

---

# License

Specify your preferred open-source license.

Examples:

- MIT
- Apache 2.0
- BSD-3-Clause

---

## About

**Yadra Qwen3 Vulkan Inference Engine** is a research-focused inference engine designed to explore how far modern mobile GPUs can be pushed for large language model inference.

Originally developed for desktop GPUs and later optimized for Android, the engine shares the same inference core across all supported platforms, while only the Vulkan backend is tuned for each GPU architecture.
