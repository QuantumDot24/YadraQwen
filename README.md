# Yadra Qwen3 Vulkan Inference Engine

A high‑performance, Vulkan‑accelerated inference engine for Qwen 3 0.6B on Android.  
Runs entirely on the GPU of a **Mali G615 MC6** using custom compute shaders and quantized weights (Q4_K / Q6_K).

---

## Features

- **Vulkan Compute Backend** – zero CPU intervention during inference; all operations run as GPU compute shaders.
- **Qwen 3 (0.6B) native support** – GGUF model loading, Q4_K / Q6_K quantised weight execution, RMSNorm, RoPE, GQA, SiLU‑gated FFN.
- **KV Cache** – sliding window cache implemented on‑device for autoregressive decoding.
- **Graph Capture Optimisation** – the first decode step captures a Vulkan compute graph, which is compiled and reused for all subsequent tokens, minimising driver overhead.
- **GPU‑side Sampling** – argmax (greedy) and temperature + top‑p sampling run directly on the GPU via custom dispatches.
- **BPE Tokenizer** – compatible with `tokenizer3.json`, handles special tokens, merges, and byte‑level fallback.
- **Streaming Output** – generation calls a user callback for each new token.
- **Detailed Performance Metrics** – prefill & decode timing information is exposed via `GenerationStats` (actual throughput numbers to be profiled later).

---

## Architecture Overview

The engine is organised around a few key components:

| Component            | Responsibility                                                                                   |
|----------------------|--------------------------------------------------------------------------------------------------|
| `Qwen3Config`        | Model hyperparameters parsed from the GGUF metadata.                                             |
| `Qwen3BPETokenizer`  | Tokenisation / de‑tokenisation using BPE merges and a vocabulary.                                |
| `KVCache`            | Manages per‑layer key/value tensors, pre‑allocated for the maximum sequence length.              |
| `Qwen3Engine`        | Orchestrates the full inference pipeline: load → prefill → decode loop.                          |
| `Tensor` / Vulkan Executor | The underlying Vulkan runtime. Provides tensor operations (matmul, norm, attention…) and graph capture. |

---

## Inference Pipeline

![Inference Pipeline](docs/pipeline.svg)

The diagram below (Graphviz `dot` source) illustrates the data flow during generation.  
*(If you cannot render it, the description follows.)*

```dot
digraph G {
    rankdir=TB;
    node [shape=box, style=rounded, fontname="Helvetica"];
    
    subgraph cluster_host {
        label="Host (CPU)";
        style=dashed;
        prompt [label="Prompt\nString"];
        tokenizer [label="Qwen3BPETokenizer\nEncode"];
        callback [label="On‑Token Callback"];
    }
    
    subgraph cluster_gpu {
        label="GPU (Vulkan Compute)";
        style=dashed;
        embed [label="Token Embedding\n(Gather Q4_K)"];
        layer [label="Transformer Layer\n× N layers"];
        norm [label="Output RMSNorm"];
        head [label="LM Head\n(Linear)"];
        logits [label="Logits"];
        sample [label="GPU Sampling\nArgmax / Temp+TopP"];
        kvcache [label="KV Cache\nRead / Write"];
    }
    
    loop [label="Decode Loop\n(one token at a time)", shape=plaintext];
    
    prompt -> tokenizer -> embed [label="Token IDs"];
    embed -> layer;
    layer -> kvcache [dir=both, label="KV Cache"];
    layer -> norm -> head -> logits -> sample;
    sample -> callback [label="Token ID"];
    sample -> loop;
    loop -> embed [label="Next Token ID", style=dashed];
    
    // Weight loading
    loader [label="GGUF Weights\n(Q4_K / Q6_K)", shape=folder];
    loader -> embed;
    loader -> layer;
    loader -> norm;
    loader -> head;
}
Description
Loading – The GGUF file is parsed; all weights are uploaded to GPU‑visible buffers as quantised matrices (Q4_K / Q6_K).

Tokenisation – The input text is split into tokens using a BPE tokenizer that respects special tokens (<|im_start|>, <|im_end|>, <think>, etc.).

Prefill – The entire prompt (multiple tokens) is processed in one call to forward().

Tokens → embedding lookup (GPU gather).

For each transformer layer:

Attention: Linear projections → Per‑head RMSNorm → RoPE → KV Cache write → Attention score → Output projection.

FFN: Gate + Up projections → SiLU(Gate) × Up → Down projection.

Final RMSNorm + LM head produce logits for the last token only.

Decode – A single token is fed back in a loop.

The first decode step captures a Vulkan compute graph that includes the layer pipeline and sampling.

This graph is compiled once and reused for every subsequent token, giving a significant speedup on Mali GPUs.

Sampling (either greedy or temperature with top‑p / top‑k) is fused into the graph, returning the next token ID directly from GPU memory.

Streaming – Each generated token is passed to a user‑supplied callback; the result string is assembled via the tokenizer’s decoder.

Vulkan Backend Highlights
The engine builds on a custom Vulkan compute runtime (yadra::Tensor::executor()):

Custom Shader Dispatches
execute_with_custom_dispatch allows the engine to invoke specialised GLSL shaders (e.g., matmul_q4k, rmsnorm, rope, fused_silu_mul, attention, sample_temperature).

Graph Capture

cpp
Tensor::executor().begin_graph();
// ... record all operations ...
Tensor::executor().stop_capture();
graph->compile();
graph->execute();
This replaces per‑operation command buffer submission with a single, pre‑recorded command buffer, drastically reducing API call overhead during decode.

Persistent Mapped Buffers
Results from GPU sampling are read directly from a persistently mapped buffer (result_buf_.buffer()->map_persistent()), avoiding GPU→CPU round‑trips.

Usage
The main entry point (main.cpp) provides an interactive chat demo for desktop platforms (Windows / Linux). It can also be compiled for Android using JNI bindings (the same engine core is used on both platforms).

Command‑line Arguments
text
./qwen3_chat [model.gguf] [tokenizer3.json] [max_seq_len]
model.gguf – path to the GGUF file (default ../models/qwen/Qwen3-0.6B-Q4_K_M.gguf)

tokenizer3.json – path to the tokenizer config (default ../models/qwen/tokenizer3.json)

max_seq_len – maximum sequence length (default 2048)

Interactive Chat
Once started, type your message and press Enter. The assistant replies with streaming output.

Special commands:

/think – toggles the model’s thinking mode (adds or removes <think> tags in the prompt).

exit or quit – exits the application.

Example Session
text
╔══════════════════════════════════════════╗
║   Qwen3-0.6B Chat — Vulkan / YadraCore   ║
║   'exit' para salir                      ║
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

You: exit
Integrating into Your Own Project
cpp
#include "yadra/qwen3/qwen3_engine.hpp"

int main() {
    yadra::Qwen3Engine engine;
    engine.load("qwen3-0.6b.Q4_K_M.gguf", "tokenizer3.json", 2048);

    // Simple chat (waits for full response)
    std::string response = engine.chat("Why is the sky blue?", 256, 0.7f, false);

    // Or use the lower‑level generate() with streaming
    auto tokens = engine.generate(prompt_ids, 256, 0.7f, 0.9f,
                                  [](int32_t token) {
                                      // handle each token
                                  });

    // Inspect timings (prefill/decode milliseconds)
    auto stats = engine.last_stats();
    printf("Prefill: %.1f ms\n", stats.prefill_ms);
    printf("Decode: %.1f ms\n", stats.decode_ms);

    return 0;
}
For Android, wrap the engine in a JNI interface; the C++ logic remains identical.

Building
Prerequisites

Android NDK (r25+), C++17

Vulkan SDK (headers & validation layers)

CMake ≥ 3.18

Dependencies

nlohmann/json (header‑only, provided in third_party/)

Your Vulkan compute runtime (yadra library)

Build (Desktop Example)

bash
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build
For Android, set -DCMAKE_TOOLCHAIN_FILE=... and target arm64-v8a.

License
Specify your open‑source license here (e.g., MIT, Apache 2.0).

Contributing
We welcome contributions! Please open an issue to discuss major changes.

This engine was originally developed for PC and then ported to Android / Mali GPUs. The core inference logic is identical; only the Vulkan backend’s shader compilation and workgroup tuning are platform‑specific.
