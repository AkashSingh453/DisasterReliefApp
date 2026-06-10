package com.disasterrelief.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Emergency-appropriate color palette for the Disaster Relief application.
 *
 * Design rationale:
 * - Deep reds/crimsons for SOS/critical elements (urgency)
 * - Warm ambers for warnings and caution states
 * - Muted teals/blues for informational content and mesh status
 * - High contrast dark mode as the primary scheme (better visibility in low-light disaster conditions)
 */

// ── Primary: Deep Crimson (SOS / Emergency) ──
val CrimsonPrimary = Color(0xFFD32F2F)
val CrimsonPrimaryDark = Color(0xFFB71C1C)
val CrimsonPrimaryLight = Color(0xFFEF5350)

// ── Secondary: Teal (Mesh / Connectivity / Info) ──
val TealSecondary = Color(0xFF00897B)
val TealSecondaryDark = Color(0xFF00695C)
val TealSecondaryLight = Color(0xFF26A69A)

// ── Tertiary: Amber (Warnings / Hazards) ──
val AmberTertiary = Color(0xFFFFA000)
val AmberTertiaryDark = Color(0xFFFF8F00)
val AmberTertiaryLight = Color(0xFFFFCA28)

// ── Dark Theme Surface Colors ──
val DarkBackground = Color(0xFF0F1114)
val DarkSurface = Color(0xFF1A1D23)
val DarkSurfaceVariant = Color(0xFF242830)
val DarkSurfaceElevated = Color(0xFF2C3038)

// ── Light Theme Surface Colors ──
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EAED)

// ── Text Colors ──
val TextOnDark = Color(0xFFE8EAED)
val TextOnDarkSecondary = Color(0xFFBDC1C6)
val TextOnLight = Color(0xFF1F1F1F)
val TextOnLightSecondary = Color(0xFF5F6368)

// ── Status Colors ──
val StatusOnline = Color(0xFF4CAF50)
val StatusOffline = Color(0xFF757575)
val StatusSyncing = Color(0xFF2196F3)
val SeverityCritical = Color(0xFFD32F2F)
val SeverityHigh = Color(0xFFE64A19)
val SeverityMedium = Color(0xFFFFA000)
val SeverityLow = Color(0xFF4CAF50)
val SeverityMinor = Color(0xFF8BC34A)
