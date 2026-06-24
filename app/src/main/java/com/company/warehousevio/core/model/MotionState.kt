package com.company.warehousevio.core.model

enum class MotionState {
    IDLE,
    FORWARD,
    REVERSE,
}

enum class MotionEventType {
    START_FORWARD,
    START_REVERSE,
    BRAKE,
}
