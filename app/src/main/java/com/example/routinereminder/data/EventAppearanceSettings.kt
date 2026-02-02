package com.example.routinereminder.data

enum class EventColorDisplayCondition {
    ALWAYS,
    NEVER,
    NEXT_UPCOMING;

    fun shouldShow(isNextUpcoming: Boolean): Boolean {
        return when (this) {
            ALWAYS -> true
            NEVER -> false
            NEXT_UPCOMING -> isNextUpcoming
        }
    }

    companion object {
        fun fromName(name: String?): EventColorDisplayCondition {
            return when (name) {
                ALWAYS.name -> ALWAYS
                NEVER.name -> NEVER
                NEXT_UPCOMING.name -> NEXT_UPCOMING
                "FUTURE_EVENTS" -> ALWAYS
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

enum class PastEventColorTreatment {
    SHOW_SELECTED,
    GREYED_OUT,
    CUSTOM;

    companion object {
        fun fromName(name: String?): PastEventColorTreatment {
            return entries.find { it.name == name } ?: GREYED_OUT
        }
    }
}

enum class PastEventTextColorChoice {
    PRIMARY,
    SECONDARY,
    WHITE,
    EVENT_COLOR,
    CUSTOM,
    GREYED_OUT;

    companion object {
        fun fromName(name: String?): PastEventTextColorChoice {
            return when (name) {
                PRIMARY.name -> PRIMARY
                SECONDARY.name -> SECONDARY
                WHITE.name -> WHITE
                EVENT_COLOR.name -> EVENT_COLOR
                CUSTOM.name -> CUSTOM
                GREYED_OUT.name -> GREYED_OUT
                PastEventColorTreatment.SHOW_SELECTED.name -> EVENT_COLOR
                PastEventColorTreatment.GREYED_OUT.name -> GREYED_OUT
                PastEventColorTreatment.CUSTOM.name -> CUSTOM
                else -> GREYED_OUT
            }
        }
    }
}

enum class PastEventDetailTextColorChoice {
    GREYED_OUT,
    WHITE,
    CUSTOM;

    companion object {
        fun fromName(name: String?): PastEventDetailTextColorChoice {
            return when (name) {
                WHITE.name -> WHITE
                CUSTOM.name -> CUSTOM
                GREYED_OUT.name -> GREYED_OUT
                else -> GREYED_OUT
            }
        }

        fun fromTitleChoice(choice: PastEventTextColorChoice): PastEventDetailTextColorChoice {
            return when (choice) {
                PastEventTextColorChoice.WHITE -> WHITE
                PastEventTextColorChoice.CUSTOM -> CUSTOM
                PastEventTextColorChoice.GREYED_OUT -> GREYED_OUT
                else -> GREYED_OUT
            }
        }
    }
}

enum class EventBackgroundTransparency(val percent: Int, val alpha: Float) {
    PERCENT_0(0, 0f),
    PERCENT_40(40, 0.4f),
    PERCENT_30(30, 0.3f),
    PERCENT_20(20, 0.2f),
    PERCENT_10(10, 0.1f);

    companion object {
        fun fromName(name: String?): EventBackgroundTransparency {
            return when (name) {
                PERCENT_0.name,
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
