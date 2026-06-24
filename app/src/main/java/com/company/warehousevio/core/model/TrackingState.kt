package com.company.warehousevio.core.model

enum class TrackingState {
    TRACKING,
    PAUSED,
    STOPPED,
}

enum class TrackingFailureReason {
    NONE,
    INSUFFICIENT_FEATURES,
    EXCESSIVE_MOTION,
    INSUFFICIENT_LIGHT,
    CAMERA_UNAVAILABLE,
    UNKNOWN,
}
