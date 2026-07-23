# 🚀 Yadra Qwen3 Vulkan Inference Engine

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20Linux-brightgreen)](#)
[![GPU](https://img.shields.io/badge/GPU-Vulkan%20Compute%20(Mali%20G615%20MC6)-blue)](#)
[![Model](https://img.shields.io/badge/Model-Qwen%203%200.6B-orange)](#)
[![Quantization](https://img.shields.io/badge/Quantization-Q4__K%20%2F%20Q6__K-purple)](#)

A high-performance, Vulkan-accelerated inference engine for **Qwen 3 (0.6B)** optimized for mobile and desktop environments. Designed to run completely on the GPU of a **Mali G615 MC6** (and compatible Vulkan GPUs) using custom compute shaders and quantized GGUF weights.

---

## ✨ Key Features

* **Zero-CPU Vulkan Backend** — Entire inference pipeline runs as custom GPU compute shaders with zero CPU intervention during generation.
* **Native Qwen 3 (0.6B) Support** — Direct execution of GGUF models with RMSNorm, RoPE, GQA, SiLU-gated FFN, and Q4_K / Q6_K quantization.
* **On-Device KV Cache** — Sliding window cache pre-allocated on GPU for efficient autoregressive decoding.
* **Graph Capture Optimization** — Captures a Vulkan compute graph on the first decode step, reusing it for all subsequent tokens to eliminate driver overhead.
* **GPU-Side Sampling** — Argmax (greedy) and Temperature + Top-P sampling executed directly on the GPU.
* **BPE Tokenizer** — Native C++ tokenizer (`tokenizer3.json` compatible) with byte-level fallback and special token handling.
* **Streaming & Metrics** — Real-time token streaming via callbacks and detailed latency profiling via `GenerationStats`.

---

## 🏗️ Architecture Overview

The core engine is structured into modular high-performance components:

| Component | Description |
| :--- | :--- |
| **`Qwen3Config`** | Parses model architecture hyperparameters directly from GGUF metadata. |
| **`Qwen3BPETokenizer`** | Fast C++ BPE tokenizer with support for special tokens (`<think>`, `<|im_start|>`). |
| **`KVCache`** | Pre-allocated per-layer key/value tensor manager for maximum sequence length. |
| **`Qwen3Engine`** | Pipeline orchestrator handling initialization, prefill, and the decode loop. |
| **`Tensor` / Vulkan Executor** | Low-level Vulkan runtime providing custom compute dispatches and graph capture. |

---

## 🔄 Inference Pipeline

![Inference Pipeline](docs/pipeline.svg)

### Execution Steps
1.  **Model Loading:** GGUF weights uploaded directly to GPU-visible memory buffers as quantized matrices.
2.  **Tokenization:** Text converted into token IDs using standard Qwen special token rules.
3.  **Prefill Stage:** Input prompt processed in a single batched `forward()` pass.
4.  **Decode Loop:** Subsequent tokens generated iteratively using Vulkan Graph Capture for ultra-low latency.
5.  **Streaming:** Tokens streamed to host memory via persistent mapped GPU buffers.

---

## 🛠️ Vulkan Engine Highlights

* **Custom Shader Dispatches:** Hand-written GLSL shaders for key kernels (`matmul_q4k`, `rmsnorm`, `rope`, `fused_silu_mul`, `sample_temperature`).
* **Vulkan Graph Capture:** Replaces individual command submissions with pre-recorded command buffers:
    ```cpp
    Tensor::executor().begin_graph();
    // ... record inference pipeline ...
    Tensor::executor().stop_capture();
    graph->compile();
    graph->execute(); // Executed on every decode token
    ```
* **Persistent Memory Mapping:** Zero-copy token output retrieval using `map_persistent()` buffers.

---

## 💻 Quick Start & Usage

### Interactive CLI Chat

Run the interactive shell on Windows, Linux, or via Android ADB:

```bash
./qwen3_chat [model.gguf] [tokenizer3.json] [max_seq_len]
#### Example Terminal Session
```text
╔══════════════════════════════════════════╗
║   Qwen3-0.6B Chat — Vulkan / YadraCore   ║
║   Type 'exit' or 'quit' to exit          ║
╚══════════════════════════════════════════╝

You: What is the capital of France?
Assistant: The capital of France is Paris.

You: /think
[Think mode: ON]

You: Solve 2+2
Assistant: <think>
2+2=4
</think>
The answer is 4.
Note: Use /think to toggle reasoning mode (<think> tags).

🔌 C++ Integration
Easily embed the engine in your own native C++ or Android JNI application:

C++
#include "yadra/qwen3/qwen3_engine.hpp"

int main() {
    yadra::Qwen3Engine engine;
    
    // Initialize engine
    engine.load("qwen3-0.6b.Q4_K_M.gguf", "tokenizer3.json", 2048);

    // High-level blocking chat
    std::string response = engine.chat("Why is the sky blue?", 256, 0.7f, false);

    // Low-level token streaming callback
    auto tokens = engine.generate(prompt_ids, 256, 0.7f, 0.9f, [](int32_t token) {
        // Process token in real-time
    });

    // Inspect latency metrics
    auto stats = engine.last_stats();
    printf("Prefill: %.1f ms | Decode: %.1f ms\n", stats.prefill_ms, stats.decode_ms);

    return 0;
}
🔨 Building from Source
Prerequisites
C++ Compiler: C++17 support

CMake: 3.18 or higher

Vulkan SDK: Headers & Validation Layers

Android Development (Optional): NDK r25+ (for arm64-v8a builds)

Desktop Build (Windows / Linux)
Bash
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release
Android Cross-Compilation
Bash
cmake -B build-android \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26 \
  -DCMAKE_BUILD_TYPE=Release

cmake --build build-android
🤝 Contributing
We welcome contributions! Please open an issue to discuss major changes. This engine was originally developed for PC and then ported to Android / Mali GPUs. The core inference logic is identical; only the Vulkan backend’s shader compilation and workgroup tuning are platform-specific.

📄 License
Specify your open-source license here (e.g., MIT, Apache 2.0).
