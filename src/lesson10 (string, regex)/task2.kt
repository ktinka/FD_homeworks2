package `lesson10 (string, regex)`

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.Result

data class NormalizedEvent(
    val dt: LocalDateTime,
    val id: Int,
    val status: String
)

data class DeliveryInfo(
    val id: Int,
    val sentTime: LocalDateTime? = null,
    val deliveredTime: LocalDateTime? = null
)

data class DuplicateInfo(
    val id: Int,
    val sentCount: Int = 0,
    val deliveredCount: Int = 0
)

class LogNormalizer {
    private val formatARegex = Regex(
        """^\s*(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})\s*\|\s*ID:(\d+)\s*\|\s*STATUS:(\w+)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val formatBRegex = Regex(
        """^\s*TS=(\d{2})/(\d{2})/(\d{4})-(\d{2}:\d{2})\s*;\s*status=(\w+)\s*;\s*#(\d+)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val formatCRegex = Regex(
        """^\s*\[(\d{2})\.(\d{2})\.(\d{4})\s+(\d{2}:\d{2})\]\s*(\w+)\s*\(id:(\d+)\)\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun normalize(line: String): Result<NormalizedEvent> = runCatching {
        val trimmed = line.trim()

        when {
            formatARegex.matches(trimmed) -> parseFormatA(trimmed)
            formatBRegex.matches(trimmed) -> parseFormatB(trimmed)
            formatCRegex.matches(trimmed) -> parseFormatC(trimmed)
            else -> throw IllegalArgumentException("Unknown format :<")
        }
    }

    private fun parseFormatA(line: String): NormalizedEvent {
        val match = formatARegex.matchEntire(line)!!
        val date = match.groupValues[1]
        val time = match.groupValues[2]
        val id = match.groupValues[3].toInt()
        val status = match.groupValues[4].lowercase()

        return NormalizedEvent(
            dt = LocalDateTime.parse("$date $time", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            id = id,
            status = status
        )
    }

    private fun parseFormatB(line: String): NormalizedEvent {
        val match = formatBRegex.matchEntire(line)!!
        val day = match.groupValues[1]
        val month = match.groupValues[2]
        val year = match.groupValues[3]
        val time = match.groupValues[4]
        val status = match.groupValues[5].lowercase()
        val id = match.groupValues[6].toInt()

        return NormalizedEvent(
            dt = LocalDateTime.parse("$year-$month-$day $time", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            id = id,
            status = status
        )
    }

    private fun parseFormatC(line: String): NormalizedEvent {
        val match = formatCRegex.matchEntire(line)!!
        val day = match.groupValues[1]
        val month = match.groupValues[2]
        val year = match.groupValues[3]
        val time = match.groupValues[4]
        val status = match.groupValues[5].lowercase()
        val id = match.groupValues[6].toInt()

        return NormalizedEvent(
            dt = LocalDateTime.parse("$year-$month-$day $time", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            id = id,
            status = status
        )
    }
}

class DeliveryAnalyzer {
    private val normalizer = LogNormalizer()

    fun analyze(logs: List<String>): Report {
        val (validEvents, brokenLines) = normalizeLogs(logs)

        val deliveryInfo = calculateDeliveryTimes(validEvents)

        return generateReport(deliveryInfo, brokenLines)
    }

    private fun normalizeLogs(logs: List<String>): Pair<List<NormalizedEvent>, List<String>> {
        val validEvents = mutableListOf<NormalizedEvent>()
        val brokenLines = mutableListOf<String>()

        logs.forEach { line ->
            val result = normalizer.normalize(line)
            val value = result.getOrNull()
            if (value != null) {
                validEvents.add(value)
            } else {
                brokenLines.add(line)
            }
        }

        return Pair(validEvents, brokenLines)
    }

    private fun calculateDeliveryTimes(events: List<NormalizedEvent>): Map<Int, DeliveryInfo> {
        val deliveries = mutableMapOf<Int, DeliveryInfo>()
        val duplicates = mutableMapOf<Int, DuplicateInfo>()

        events.groupBy { it.id }.forEach { (id, idEvents) ->
            val sentEvents = idEvents.filter { it.status == "sent" }
            val deliveredEvents = idEvents.filter { it.status == "delivered" }

            duplicates[id] = DuplicateInfo(
                id = id,
                sentCount = sentEvents.size,
                deliveredCount = deliveredEvents.size
            )

            deliveries[id] = DeliveryInfo(
                id = id,
                sentTime = sentEvents.minByOrNull { it.dt }?.dt,
                deliveredTime = deliveredEvents.maxByOrNull { it.dt }?.dt
            )
        }

        return deliveries
    }

    private fun generateReport(
        deliveries: Map<Int, DeliveryInfo>,
        brokenLines: List<String>
    ): Report {
        val duplicates = deliveries.map { (id, info) ->
            DuplicateInfo(
                id = id,
                sentCount = if (info.sentTime != null) 1 else 0,
                deliveredCount = if (info.deliveredTime != null) 1 else 0
            )
        }.filter { it.sentCount > 1 || it.deliveredCount > 1 }

        val completeDeliveries = deliveries.values.filter { info ->
            info.sentTime != null &&
                    info.deliveredTime != null &&
                    info.deliveredTime > info.sentTime
        }

        val durations = completeDeliveries.associate { info ->
            val minutes = ChronoUnit.MINUTES.between(info.sentTime, info.deliveredTime)
            info.id to minutes
        }

        val sortedById = durations.toList().sortedByDescending { it.second }


        val longest = sortedById.maxByOrNull { it.second }


        val violators = durations.filter { it.value > 20 }


        val incomplete = deliveries.values.filter { info ->
            info.sentTime == null || info.deliveredTime == null
        }.map { it.id }


        val timeErrors = deliveries.values.filter { info ->
            info.sentTime != null &&
                    info.deliveredTime != null &&
                    info.deliveredTime <= info.sentTime
        }.map { it.id }


        val hourlyStats = deliveries.values
            .filter { it.deliveredTime != null }
            .groupBy { it.deliveredTime!!.hour }
            .mapValues { it.value.size }
            .toSortedMap()

        return Report(
            durations = durations,
            sortedById = sortedById,
            longest = longest,
            violators = violators,
            incomplete = incomplete,
            timeErrors = timeErrors,
            duplicates = duplicates,
            hourlyStats = hourlyStats,
            brokenLines = brokenLines
        )
    }
}

data class Report(
    val durations: Map<Int, Long>,
    val sortedById: List<Pair<Int, Long>>,
    val longest: Pair<Int, Long>?,
    val violators: Map<Int, Long>,
    val incomplete: List<Int>,
    val timeErrors: List<Int>,
    val duplicates: List<DuplicateInfo>,
    val hourlyStats: Map<Int, Int>,
    val brokenLines: List<String>
)

fun main() {
    val logs = listOf(
            "2026-01-22 09:14 | ID:042 | STATUS:sent",
            "TS=22/01/2026-09:27; status=delivered; #042",
            "2026-01-22 09:10 | ID:043 | STATUS:sent",
            "2026-01-22 09:18 | ID:043 | STATUS:delivered",
            "TS=22/01/2026-09:05; status=sent; #044",
            "[22.01.2026 09:40] delivered (id:044)",
            "2026-01-22 09:20 | ID:045 | STATUS:sent",
            "[22.01.2026 09:33] delivered (id:045)",
            "   ts=22/01/2026-09:50; STATUS=Sent; #046   ",
            " [22.01.2026 10:05]   DELIVERED   (ID:046) "
    )

    println("Анализ логов")

    val analyzer = DeliveryAnalyzer()
    val report = analyzer.analyze(logs)

    if (report.brokenLines.isNotEmpty()) {
        println("Найдено ${report.brokenLines.size} битых строк:")
        report.brokenLines.forEach { println(" $it") }
    } else {
        println("Нет битых строк")
    }

    println("Длительность доставки по ID (отсортировано по убыванию)")
    report.sortedById.forEach { (id, minutes) ->
        println("ID:$id — ${minutes} мин.")
    }

    report.longest?.let { (id, minutes) ->
        println("\nСамый долгий заказ. ID:$id (${minutes} мин.)")
    }

    if (report.violators.isNotEmpty()) {
        println("\nОбн")
        report.violators.forEach { (id, minutes) ->
            println("ID:$id — ${minutes} мин.")
        }
    } else {
        println("\nВсё ок")
    }

    if (report.incomplete.isNotEmpty()) {
        println("\nНеполные заказы (нет sent или delivered)")
        report.incomplete.forEach { id ->
            println("ID:$id")
        }
    }

    if (report.timeErrors.isNotEmpty()) {
        println("\nОшибки времени (delivered раньше sent)")
        report.timeErrors.forEach { id ->
            println("ID:$id")
        }
    }

    if (report.duplicates.isNotEmpty()) {
        println("\nДетектор дублей")
        report.duplicates.forEach { dup ->
            println("ID:${dup.id} — sent: ${dup.sentCount}, delivered: ${dup.deliveredCount}")
        }
    }

    println("\nСводка по часам (количество доставок)")
    report.hourlyStats.forEach { (hour, count) ->
        println("${hour.toString().padStart(2, '0')}:00 — ${count} доставок")
    }
}