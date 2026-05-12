package com.voxpocket.util

object Constants {
    // Server
    const val DEFAULT_SERVER_PORT = 8080
    const val SERVER_STARTUP_TIMEOUT_MS = 30000L
    const val HEALTH_CHECK_INTERVAL_MS = 500L
    const val HEALTH_CHECK_MAX_RETRIES = 60

    // Model
    const val DEFAULT_MAX_TOKENS = 1024
    const val DEFAULT_TEMPERATURE = 0.7f
    const val DEFAULT_TOP_P = 0.9f
    const val DEFAULT_TOP_K = 40
    const val DEFAULT_REPEAT_PENALTY = 1.1f
    const val DEFAULT_CONTEXT_SIZE = 2048

    // Context Management
    const val DEFAULT_MAX_CONTEXT_TOKENS = 1800
    const val CONTEXT_COMPRESSION_THRESHOLD = 0.8

    // UI
    const val MESSAGE_MAX_LINES = 1000
    const val STREAMING_OUTPUT_DELAY_MS = 50L
    const val SCROLL_ANIMATION_DURATION_MS = 300L

    // Storage
    const val DATABASE_NAME = "voxpocket_db"
    const val PREFERENCES_NAME = "voxpocket_prefs"

    // Animation
    const val LOADING_DURATION_MS = 200L
    const val FADE_IN_DURATION_MS = 150L
    const val SLIDE_IN_DURATION_MS = 200L

    // Network
    const val REQUEST_TIMEOUT_SECONDS = 60L
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 30L
}
