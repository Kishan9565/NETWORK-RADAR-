package com.networkradar.core.designsystem

import androidx.compose.ui.graphics.Color

// Diagnostics Palette
val DarkBackground = Color(0xFF0A0C10)
val SurfaceDark = Color(0xFF161B22)
val ElectricCyan = Color(0xFF00E5FF)
val SignalGreen = Color(0xFF00E676)
val SignalAmber = Color(0xFFFFB300)
val SignalRed = Color(0xFFF44336)
val NeutralGray = Color(0xFF8B949E)

// Semantic Tokens
val SignalExcellent = SignalGreen
val SignalGood = Color(0xFF66BB6A)
val SignalFair = SignalAmber
val SignalPoor = SignalRed
val SignalUnavailable = NeutralGray

// Standard Compose colors (Dark Theme)
val Primary = ElectricCyan
val OnPrimary = Color.Black
val Secondary = Color(0xFF58A6FF)
val OnSecondary = Color.White
val Tertiary = SignalGreen
val Background = DarkBackground
val OnBackground = Color(0xFFC9D1D9)
val Surface = SurfaceDark
val OnSurface = Color(0xFFC9D1D9)
val SurfaceVariant = Color(0xFF21262D)
val OnSurfaceVariant = Color(0xFF8B949E)
