package com.example.routinereminder.data

enum class EventDialogField(val key: String) {
    TITLE("title"),
    NOTES("notes"),
    START("start"),
    DESTINATION("destination"),
    TIME("time"),
    DURATION("duration"),
    EVENT_SET("event_set"),
    EVENT_COLOR("event_color"),
    REPEAT("repeat"),
    DATE_DETAILS("date_details"),
    CALENDAR("calendar"),
    CALENDAR_TARGET("calendar_target"),
    NOTIFICATION("notification"),
    NOTIFICATION_DETAILS("notification_details"),
    REMINDER_OPTIONS("reminder_options");

    companion object {
        fun fromKey(key: String): EventDialogField? = entries.firstOrNull { it.key == key }
    }
}

data class EventDialogFieldOption(
    val field: EventDialogField,
    val enabled: Boolean
) {
    companion object {
        private val requiredFields = emptySet<EventDialogField>()
        private val linkedFieldGroups = listOf(
            listOf(EventDialogField.REPEAT, EventDialogField.DATE_DETAILS)
        )

        private val parentByChildField: Map<EventDialogField, EventDialogField> = mapOf(
            EventDialogField.CALENDAR_TARGET to EventDialogField.CALENDAR,
            EventDialogField.NOTIFICATION_DETAILS to EventDialogField.NOTIFICATION,
            EventDialogField.REMINDER_OPTIONS to EventDialogField.NOTIFICATION
        )
        private val childByParentField: Map<EventDialogField, List<EventDialogField>> =
            parentByChildField.entries
                .groupBy({ it.value }, { it.key })

        fun isRequired(field: EventDialogField): Boolean = field in requiredFields

        fun enforceRequired(fields: List<EventDialogFieldOption>): List<EventDialogFieldOption> {
            val requiredEnforced = fields.map { option ->
                if (isRequired(option.field)) option.copy(enabled = true) else option
            }
            return linkedFieldGroups.fold(requiredEnforced) { currentFields, linkedGroup ->
                val controllingField = linkedGroup.firstOrNull() ?: return@fold currentFields
                val controllingState = currentFields.firstOrNull { it.field == controllingField }?.enabled
                    ?: return@fold currentFields
                currentFields.map { option ->
                    if (option.field in linkedGroup && !isRequired(option.field)) {
                        option.copy(enabled = controllingState)
                    } else {
                        option
                    }
                }
            }
        }

        fun applyLinkedFieldRules(
            fields: List<EventDialogFieldOption>,
            changedField: EventDialogField,
            enabled: Boolean
        ): List<EventDialogFieldOption> {
            val linkedGroup = linkedFieldGroups.firstOrNull { changedField in it } ?: return fields
            return fields.map { option ->
                if (option.field in linkedGroup && !isRequired(option.field)) {
                    option.copy(enabled = enabled)
                } else {
                    option
                }
            }
        }

        fun enforceDependencies(fields: List<EventDialogFieldOption>): List<EventDialogFieldOption> {
            val enabledByField = fields.associate { it.field to it.enabled }.toMutableMap()

            parentByChildField.forEach { (child, parent) ->
                if (enabledByField[child] == true) {
                    enabledByField[parent] = true
                }
            }

            childByParentField.forEach { (parent, children) ->
                if (enabledByField[parent] == false) {
                    children.forEach { child -> enabledByField[child] = false }
                }
            }

            // Calendar target is merged with Calendar in settings and should always mirror it.
            enabledByField[EventDialogField.CALENDAR_TARGET] = enabledByField[EventDialogField.CALENDAR] ?: true

            return fields.map { option ->
                option.copy(enabled = enabledByField[option.field] ?: option.enabled)
            }
        }

        fun applyRules(fields: List<EventDialogFieldOption>): List<EventDialogFieldOption> {
            return enforceDependencies(enforceRequired(fields))
        }

        fun defaults(): List<EventDialogFieldOption> = listOf(
            EventDialogFieldOption(EventDialogField.TITLE, false),
            EventDialogFieldOption(EventDialogField.NOTES, true),
            EventDialogFieldOption(EventDialogField.TIME, false),
            EventDialogFieldOption(EventDialogField.DURATION, true),
            EventDialogFieldOption(EventDialogField.REPEAT, true),
            EventDialogFieldOption(EventDialogField.DATE_DETAILS, false),
            EventDialogFieldOption(EventDialogField.START, false),
            EventDialogFieldOption(EventDialogField.DESTINATION, true),
            EventDialogFieldOption(EventDialogField.CALENDAR, true),
            EventDialogFieldOption(EventDialogField.CALENDAR_TARGET, true),
            EventDialogFieldOption(EventDialogField.NOTIFICATION, true),
            EventDialogFieldOption(EventDialogField.NOTIFICATION_DETAILS, true),
            EventDialogFieldOption(EventDialogField.REMINDER_OPTIONS, false),
            EventDialogFieldOption(EventDialogField.EVENT_SET, false),
            EventDialogFieldOption(EventDialogField.EVENT_COLOR, false)
        ).let(::applyRules)
    }
}
