package com.example.glarmto.data.util

import com.example.glarmto.data.local.entity.NutritionEntity
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.local.entity.WaterEntity
import com.example.glarmto.data.local.entity.WorkoutEntity

/**
 * JSON/CSV export without Android [org.json] types so the same code runs on JVM unit tests and on device.
 */
object GlarmToExport {

    fun toJson(username: String, user: UserEntity?, workouts: List<WorkoutEntity>, nutrition: List<NutritionEntity>, water: List<WaterEntity>): String = buildString {
        append("{\n")
        append("  \"exportVersion\": 1,\n")
        append("  \"username\": ").append(jsonString(username)).append(",\n")
        if (user != null) {
            append("  \"user\": ").append(userToJsonString(user)).append(",\n")
        }
        append("  \"workouts\": ").append(jsonArray(workouts.map { workoutToJsonString(it) })).append(",\n")
        append("  \"nutrition\": ").append(jsonArray(nutrition.map { nutritionToJsonString(it) })).append(",\n")
        append("  \"water\": ").append(jsonArray(water.map { waterToJsonString(it) }))
        append("\n}")
    }

    fun toCsv(workouts: List<WorkoutEntity>, nutrition: List<NutritionEntity>, water: List<WaterEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("# GlarmTo export")
        sb.appendLine("type,id,username,dateInMillis,extras")
        workouts.forEach { w ->
            sb.appendLine(
                listOf(
                    "workout",
                    w.id,
                    w.username,
                    w.dateInMillis,
                    "${w.exerciseName};${w.weight};${w.reps};${w.sessionId ?: ""};${w.rpe ?: ""}"
                ).joinToString(",") { cell ->
                    val s = cell.toString()
                    if (s.contains(',') || s.contains('"')) "\"${s.replace("\"", "\"\"")}\"" else s
                }
            )
        }
        nutrition.forEach { n ->
            sb.appendLine("nutrition,${n.id},${n.username},${n.dateInMillis},${n.foodName};${n.calories}")
        }
        water.forEach { w ->
            sb.appendLine("water,${w.id},${w.username},${w.dateInMillis},${w.amountMl}")
        }
        return sb.toString()
    }

    private fun userToJsonString(u: UserEntity): String = jsonObject(
        "username" to jsonString(u.username),
        "dailyGoal" to u.dailyGoal.toString(),
        "macroProteinPct" to u.macroProteinPct.toString(),
        "macroCarbPct" to u.macroCarbPct.toString(),
        "macroFatPct" to u.macroFatPct.toString(),
        "dailyWaterGoalMl" to u.dailyWaterGoalMl.toString()
    )

    private fun workoutToJsonString(w: WorkoutEntity): String = jsonObject(
        "id" to w.id.toString(),
        "exerciseName" to jsonString(w.exerciseName),
        "weight" to w.weight.toString(),
        "reps" to w.reps.toString(),
        "dateInMillis" to w.dateInMillis.toString(),
        "username" to jsonString(w.username),
        "sessionId" to (w.sessionId?.toString() ?: "null"),
        "rpe" to (w.rpe?.toString() ?: "null")
    )

    private fun nutritionToJsonString(n: NutritionEntity): String = jsonObject(
        "id" to n.id.toString(),
        "foodName" to jsonString(n.foodName),
        "calories" to n.calories.toString(),
        "dateInMillis" to n.dateInMillis.toString(),
        "username" to jsonString(n.username)
    )

    private fun waterToJsonString(w: WaterEntity): String = jsonObject(
        "id" to w.id.toString(),
        "amountMl" to w.amountMl.toString(),
        "dateInMillis" to w.dateInMillis.toString(),
        "username" to jsonString(w.username)
    )

    private fun jsonObject(entries: List<Pair<String, String>>): String =
        entries.joinToString(prefix = "{", postfix = "}", separator = ", ") { (k, v) -> "${jsonString(k)}: $v" }

    private fun jsonObject(vararg pairs: Pair<String, String>): String = jsonObject(pairs.toList())

    private fun jsonArray(elements: List<String>): String =
        elements.joinToString(prefix = "[", postfix = "]", separator = ", ") { it }

    /** JSON string literal (quoted). */
    private fun jsonString(s: String): String = buildString(s.length + 2) {
        append('"')
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }
}
