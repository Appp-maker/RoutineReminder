package com.example.routinereminder.data

enum class EventColorDisplayCondition {
    ALWAYS,
    DONE,
    PAST,
    DONE_OR_PAST,
    NEVER;

    fun shouldShow(isDone: Boolean, isPast: Boolean): Boolean {
        return when (this) {
            ALWAYS -> true
            DONE -> isDone
            PAST -> isPast
            DONE_OR_PAST -> isDone || isPast
            NEVER -> false
        }
    }

    companion object {
        fun fromName(name: String?): EventColorDisplayCondition {
            return entries.find { it.name == name } ?: ALWAYS
        }
    }
}

enum class EventTitleColorChoice {
    PRIMARY,
    SECONDARY,
    EVENT_COLOR,
    CUSTOM;

    companion object {
        fun fromName(name: String?): EventTitleColorChoice {
            return entries.find { it.name == name } ?: PRIMARY
        }
    }
}
