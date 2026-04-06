package com.complexsoft.yadraqwen.ui.theme

import androidx.compose.ui.graphics.Color

// ── Fondos ─────────────────────────────────────────────────────────────────
val Void          = Color(0xFF080709)   // fondo principal — casi negro con tinte cálido
val Surface       = Color(0xFF0F0D11)   // superficies
val Elevated      = Color(0xFF17141C)   // elevadas
val Border        = Color(0xFF241F2E)   // bordes sutiles

// ── Vulkan Red ──────────────────────────────────────────────────────────────
val VulkanRed     = Color(0xFFD93025)   // rojo Vulkan — crimsón saturado
val VulkanRedDim  = Color(0xFF1F0C0B)   // fondo burbuja / dim
val VulkanRedGlow = Color(0x44D93025)   // glow

// ── Qwen Purple ─────────────────────────────────────────────────────────────
val QwenPurple     = Color(0xFF8B5CF6)  // morado Qwen
val QwenPurpleDim  = Color(0xFF150F26)  // fondo burbuja usuario
val QwenPurpleGlow = Color(0x448B5CF6)

// ── Gradiente de marca (Vulkan → Qwen) ──────────────────────────────────────
val GradientStart = VulkanRed
val GradientEnd   = QwenPurple

// ── Thinking Mode — mismo amber, más suave ───────────────────────────────────
val ThinkAmber    = Color(0xFFF59E0B)
val ThinkAmberDim = Color(0xFF1C1408)

// ── Texto ────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFE8E2F0)
val TextSecondary = Color(0xFF5A5068)
val TextTertiary  = Color(0xFF2E2840)

// ── Estado ───────────────────────────────────────────────────────────────────
val StatusReady   = Color(0xFF34D399)
val StatusError   = Color(0xFFD93025)

// ── Alias de compatibilidad (para no romper referencias existentes) ──────────
val NeuralVoid       = Void
val NeuralSurface    = Surface
val NeuralElevated   = Elevated
val NeuralBorder     = Border
val PlasmaViolet     = QwenPurple
val PlasmaVioletDim  = QwenPurpleDim
val PlasmaVioletGlow = QwenPurpleGlow
val BioTeal          = VulkanRed
val BioTealDim       = VulkanRedDim
val BioTealGlow      = VulkanRedGlow
val SolarAmber       = ThinkAmber
val SolarAmberDim    = ThinkAmberDim
val SolarAmberGlow   = Color(0x44F59E0B)