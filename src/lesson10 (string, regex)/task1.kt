package `lesson10 (string, regex)`

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun main() {
    println("Строки + регулярные выражения")
    task1()

    println("\nДаты + коллекции")
    task2()

    println("\nКоллекции + строки")
    task3()

    println("\nРегулярные выражения: проверка формата")
    task4()

    println("\nСтроки: нормализация пробелов")
    task5()

    println("\nДаты: разница между двумя датами")
    task6()

    println("\nКоллекции: группировка по ключу")
    task7()

    println("\nРегулярные выражения + даты: извлечение времени из текста")
    task8()
}

fun task1() {
    val data = listOf(
        "Name: Ivan, score=17",
        "Name: Olga, score=23",
        "Name: Max, score=5"
    )

    val regex = """Name: (\w+), score=(\d+)""".toRegex()

    val pairs = data.mapNotNull { line ->
        regex.find(line)?.destructured?.let { (name, score) ->
            name to score.toInt()
        }
    }

    val (maxName, maxScore) = pairs.maxBy { it.second }
    println("Максимальный score у $maxName: $maxScore")
}

fun task2() {
    val dates = listOf(
        "2026-01-22",
        "2026-02-01",
        "2025-12-31",
        "2026-01-05"
    )

    val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    val sortedDates = dates.map { LocalDate.parse(it, formatter) }.sorted()
    println("Отсортированные даты: $sortedDates")

    val jan2026Count = sortedDates.count {
        it.year == 2026 && it.monthValue == 1
    }
    println("Даты в январе 2026: $jan2026Count")
}

fun task3() {
    val text = "apple orange apple banana orange apple"

    val wordCount = text.split(" ")
        .groupingBy { it }
        .eachCount()

    val result = wordCount.filter { it.value > 1 }
        .keys
        .sorted()

    println("Слова, встретившиеся больше одного раза: $result")
}

fun task4() {
    val data = listOf("A-123", "B-7", "AA-12", "C-001", "D-99x")

    val regex = """^[A-Z]-\d{1,3}$""".toRegex()

    val filtered = data.filter { it.matches(regex) }
    println("Отфильтрованный список: $filtered")
}

fun task5() {
    val data = listOf(
        "  Hello   world  ",
        "A   B    C",
        "   one"
    )

    val normalized = data.map { str ->
        str.trim().replace(Regex("\\s+"), " ")
    }

    println("Нормализованные строки:")
    normalized.forEach { println("'$it'") }
}

fun task6() {
    val datePairs = listOf(
        "2026-01-01" to "2026-01-10",
        "2025-12-31" to "2026-01-01",
        "2026-02-01" to "2026-01-22"
    )

    val differences = datePairs.map { (first, second) ->
        val date1 = LocalDate.parse(first)
        val date2 = LocalDate.parse(second)
        ChronoUnit.DAYS.between(date1, date2)
    }

    println("Разница в днях: $differences")
}

fun task7() {
    val data = listOf(
        "math:Ivan",
        "bio:Olga",
        "math:Max",
        "bio:Ivan",
        "cs:Olga"
    )

    val grouped = data.map { it.split(":") }
        .groupBy(
            keySelector = { it[0] },
            valueTransform = { it[1] }
        )

    println("Группировка по предметам:")
    grouped.forEach { (subject, students) ->
        println("$subject -> $students")
    }
}

fun task8() {
    val data = listOf(
        "Start at 2026/01/22 09:14",
        "No time here",
        "End: 22-01-2026 18:05"
    )

    val format1 = """(\d{4})/(\d{2})/(\d{2}) (\d{2}):(\d{2})""".toRegex()
    val format2 = """(\d{2})-(\d{2})-(\d{4}) (\d{2}):(\d{2})""".toRegex()

    val result = data.mapNotNull { line ->
        when {
            format1.matches(line) -> {
                val (year, month, day, hour, minute) = format1.find(line)!!.destructured
                "$year-$month-$day $hour:$minute"
            }
            format2.matches(line) -> {
                val (day, month, year, hour, minute) = format2.find(line)!!.destructured
                "$year-$month-$day $hour:$minute"
            }
            else -> null
        }
    }

    println("Извлеченные даты и время:")
    result.forEach { println(it) }
}
