package com.example.routinereminder.data

enum class EventColorDisplayCondition {
    ALWAYS,
    NEVER,
    NEXT_UPCOMING,
    FUTURE_EVENTS;

    fun shouldShow(isNextUpcoming: Boolean, isFutureEvent: Boolean): Boolean {
        return when (this) {
            ALWAYS -> true
            NEVER -> false
            NEXT_UPCOMING -> isNextUpcoming
            FUTURE_EVENTS -> isFutureEvent
        }
    }

    companion object {
        fun fromName(name: String?): EventColorDisplayCondition {
            return when (name) {
                ALWAYS.name -> ALWAYS
                NEVER.name -> NEVER
                NEXT_UPCOMING.name -> NEXT_UPCOMING
                FUTURE_EVENTS.name -> FUTURE_EVENTS
                "DONE",
                "PAST",
                "DONE_OR_PAST" -> ALWAYS
                else -> ALWAYS
            }
        }
    }
}

enum class EventTitleColorChoice {
    PRIMARY,
    SECONDARY,
    WHITE,
    EVENT_COLOR,
    CUSTOM;

    companion object {
        fun fromName(name: String?): EventTitleColorChoice {
            return entries.find { it.name == name } ?: PRIMARY
        }
    }
}

enum class EventBackgroundTransparency(val percent: Int, val alpha: Float) {
    PERCENT_40(40, 0.4f),
    PERCENT_30(30, 0.3f),
    PERCENT_20(20, 0.2f),
    PERCENT_10(10, 0.1f);

    companion object {
        fun fromName(name: String?): EventBackgroundTransparency {
            return when (name) {
                PERCENT_40.name,
                PERCENT_30.name,
                PERCENT_20.name,
                PERCENT_10.name -> entries.first { it.name == name }
                "PERCENT_60",
                "PERCENT_80",
                "PERCENT_100" -> PERCENT_40
                else -> PERCENT_20
            }
        }
    }
}
