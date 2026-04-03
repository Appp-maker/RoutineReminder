package com.example.routinereminder.data

enum class EventDialogField(val key: String) {
    NOTES("notes"),
    START("start"),
    DESTINATION("destination"),
    TIME("time"),
    DURATION("duration"),
    EVENT_SET("event_set"),
    EVENT_COLOR("event_color"),
    REPEAT("repeat"),
    CALENDAR("calendar"),
    NOTIFICATION("notification");

    companion object {
        fun fromKey(key: String): EventDialogField? = entries.firstOrNull { it.key == key }
    }
}

data class EventDialogFieldOption(
    val field: EventDialogField,
    val enabled: Boolean
) {
    companion object {
        fun defaults(): List<EventDialogFieldOption> = listOf(
            EventDialogFieldOption(EventDialogField.NOTES, true),
            EventDialogFieldOption(EventDialogField.START, true),
            EventDialogFieldOption(EventDialogField.DESTINATION, true),
            EventDialogFieldOption(EventDialogField.TIME, true),
            EventDialogFieldOption(EventDialogField.DURATION, true),
            EventDialogFieldOption(EventDialogField.EVENT_SET, true),
            EventDialogFieldOption(EventDialogField.EVENT_COLOR, true),
            EventDialogFieldOption(EventDialogField.REPEAT, true),
            EventDialogFieldOption(EventDialogField.CALENDAR, true),
            EventDialogFieldOption(EventDialogField.NOTIFICATION, true)
        )
    }
}
