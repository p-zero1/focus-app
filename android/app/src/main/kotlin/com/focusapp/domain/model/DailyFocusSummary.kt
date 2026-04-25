package com.focusapp.domain.model

/**
 * Aggregate summary of all focus sessions for a single calendar day.
 * Returned by analytics DAO queries and consumed by [AnalyticsViewModel].
 *
 * [date] is an ISO-8601 date string "YYYY-MM-DD" in device local time.
 * Column names are matched by Room when this is used as a query result POJO.
 */
data class DailyFocusSummary(
    val date: String,
    val totalMinutes: Int,
    val completedSessions: Int,
    val startedSessions: Int,
    val totalDistractionMinutes: Int,
)
