package org.offlinemesh.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AirLinkColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryDark: Color,
    val cyan: Color,
    val blue: Color,
    val red: Color,
    val amber: Color
)

// 1. Dark Mode (OLED Obsidian)
val DarkAirLinkColors = AirLinkColors(
    isDark = true,
    background = Color(0xFF090C11),
    surface = Color(0xFF121721),
    surfaceElevated = Color(0xFF181F2C),
    surfaceVariant = Color(0xFF1F293A),
    cardBorder = Color(0x1AFFFFFF), // Subtle 10% white border
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    primary = Color(0xFF10B981),
    primaryDark = Color(0xFF059669),
    cyan = Color(0xFF06B6D4),
    blue = Color(0xFF3B82F6),
    red = Color(0xFFEF4444),
    amber = Color(0xFFF59E0B)
)

// 2. Light Mode (Crisp Minimalist Paper & Slate)
val LightAirLinkColors = AirLinkColors(
    isDark = false,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFFE2E8F0),
    cardBorder = Color(0x1F0F172A), // Subtle 12% dark border
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    primary = Color(0xFF059669),
    primaryDark = Color(0xFF047857),
    cyan = Color(0xFF0284C7),
    blue = Color(0xFF2563EB),
    red = Color(0xFFDC2626),
    amber = Color(0xFFD97706)
)

val LocalAirLinkColors = compositionLocalOf { DarkAirLinkColors }

// Global Palette Constants
val MeshBackgroundDark = Color(0xFF090C11)
val MeshSurfaceDark = Color(0xFF121721)
val MeshSurfaceElevated = Color(0xFF181F2C)
val MeshSurfaceVariantDark = Color(0xFF1F293A)
val MeshCardBorder = Color(0x1AFFFFFF)

val MeshPrimaryEmerald = Color(0xFF10B981)
val MeshPrimaryLight = Color(0xFF34D399)
val MeshPrimaryDark = Color(0xFF059669)
val MeshCyan = Color(0xFF06B6D4)
val MeshBlue = Color(0xFF3B82F6)
val MeshIndigo = Color(0xFF6366F1)
val MeshAmber = Color(0xFFF59E0B)
val MeshRed = Color(0xFFEF4444)

val MeshTextPrimary = Color(0xFFF8FAFC)
val MeshTextSecondary = Color(0xFF94A3B8)
val MeshTextMuted = Color(0xFF64748B)
