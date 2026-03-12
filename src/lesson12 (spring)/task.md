# Семинар: Многопоточность и корутины в Kotlin

---

## Часть 1. Потоки (Thread)

### Задание 1. Создание потоков
Создайте 3 потока с именами "Thread-A", "Thread-B", "Thread-C". Каждый поток должен вывести своё имя 5 раз с задержкой 500мс.

```kotlin
//import kotlin.concurrent.thread

object CreateThreads {
    fun run(): List<Thread> {
        val threads = mutableListOf<Thread>()
        val threadNames = listOf("Thread-A", "Thread-B", "Thread-C")

        threadNames.forEach { name ->
            val t = thread(start = false, name = name) {
                repeat(5) { i ->
                    println("${Thread.currentThread().name} - вывод ${i + 1}")
                    Thread.sleep(500)
                }
            }
            threads.add(t)
            t.start()
        }

        threads.forEach { it.join() }
        return threads
    }
}

// Тестирование
fun main() {
    println("Задание 1: Создание потоков")
    CreateThreads.run()
}
```

### Задание 2. Race condition
Создайте переменную `counter = 0`. Запустите 10 потоков, каждый из которых увеличивает counter на 1000. Выведите финальное значение и объясните результат.

```kotlin
object RaceCondition {
    @Volatile
    private var counter = 0

    fun run(): Int {
        counter = 0
        val threads = List(10) {
            thread {
                repeat(1000) {
                    counter++
                }
            }
        }

        threads.forEach { it.join() }
        return counter
    }
}

fun main() {
    repeat(5) {
        val result = RaceCondition.run()
        println("Попытка ${it + 1}. counter = $result")
    }
    println("Объяснение. Из-за отсутствия синхронизации потоки одновременно читают и записывают counter, это приводит к потерянным обновлениям, и в итоге результат получается неверным.")
}
```

### Задание 3. Synchronized
Исправьте задание 2 с помощью `@Synchronized` или `synchronized {}` блока, чтобы результат всегда был 10000.

```kotlin
object SynchronizedCounter {
    private var counter = 0

    @Synchronized
    private fun increment() {
        counter++
    }

    fun run(): Int {
        counter = 0
        val threads = List(10) {
            thread {
                repeat(1000) {
                    increment()
                }
            }
        }

        threads.forEach { it.join() }
        return counter
    }
}

object SynchronizedCounterBlock {
    private var counter = 0
    private val lock = Any()

    fun run(): Int {
        counter = 0
        val threads = List(10) {
            thread {
                repeat(1000) {
                    synchronized(lock) {
                        counter++
                    }
                }
            }
        }

        threads.forEach { it.join() }
        return counter
    }
}

fun main() {
    println("\nSynchronized")
    val result = SynchronizedCounter.run()
    println("С synchronized. counter = $result (всегда 10000)")
}
```

### Задание 4. Deadlock
Создайте пример deadlock с двумя ресурсами и двумя потоками. Затем исправьте его.

```kotlin
object Deadlock {
    private val resource1 = Any()
    private val resource2 = Any()

    fun runDeadlock() {

        val thread1 = thread {
            synchronized(resource1) {
                println("Thread 1 (locked resource 1)")
                Thread.sleep(100) // для thread2
                synchronized(resource2) {
                    println("Thread 1 (locked resource 2)")
                }
            }
        }

        val thread2 = thread {
            synchronized(resource2) {
                println("Thread 2 (locked resource 2)")
                Thread.sleep(100)

                synchronized(resource1) {
                    println("Thread 2 (locked resource 1)")
                }
            }
        }

        thread1.join(1000)
        thread2.join(1000)
        println("Deadlock произошел (потоки заблокированы)")
    }

    fun runFixed(): Boolean {
        println("Исправленная версия")
        var completed = false

        val thread1 = thread {
            synchronized(resource1) {
                println("Thread 1 (locked resource 1)")
                Thread.sleep(100)

                synchronized(resource2) {
                    println("Thread 1 (locked resource 2)")
                }
            }
        }

        val thread2 = thread {
            synchronized(resource1) {
                println("Thread 2 (locked resource 1)")
                Thread.sleep(100)

                synchronized(resource2) {
                    println("Thread 2 (locked resource 2)")
                }
            }
        }

        thread1.join()
        thread2.join()
        completed = true
        println("Исправлено ^^")
        return completed
    }
}

fun main() {
    println("\nDeadlock")
    Deadlock.runDeadlock()
    println()
    Deadlock.runFixed()
}
```

---

## Часть 2. Executor Framework

### Задание 5. ExecutorService
Используя `Executors.newFixedThreadPool(4)`, выполните 20 задач. Каждая задача выводит свой номер и имя потока, затем спит 200мс.

```kotlin
//import java.util.concurrent.Executors
//import java.util.concurrent.TimeUnit
object ExecutorServiceExample {
    fun run(): List<String> {
        val executor = Executors.newFixedThreadPool(4)
        val results = mutableListOf<String>()

        val tasks = List(20) { index ->
            Runnable {
                val threadName = Thread.currentThread().name
                val message = "Задача $index выполняется в $threadName"
                println(message)
                results.add(message)
                Thread.sleep(200)
            }
        }

        tasks.forEach { executor.submit(it) }

        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        return results
    }
}

fun main() {
    println("\nExecutorService")
    ExecutorServiceExample.run()
}
```

### Задание 6. Future
Используя ExecutorService и `Callable`, параллельно вычислите факториалы чисел от 1 до 10. Соберите результаты через `Future.get()`.

```kotlin
//import java.math.BigInteger
//import java.util.concurrent.Future

object FutureFactorial {
    fun run(): Map<Int, BigInteger> {
        val executor = Executors.newFixedThreadPool(4)
        val futures = mutableMapOf<Int, Future<BigInteger>>()

        for (i in 1..10) {
            val future = executor.submit<BigInteger> {
                calculateFactorial(i)
            }
            futures[i] = future
        }

        val results = futures.mapValues { (_, future) ->
            future.get()
        }

        executor.shutdown()
        return results
    }

    private fun calculateFactorial(n: Int): BigInteger {
        var result = BigInteger.ONE
        for (i in 1..n) {
            result = result.multiply(BigInteger.valueOf(i.toLong()))
        }
        return result
    }
}

// Тестирование
fun main() {
    println("\nЗадание 6")
    val factorials = FutureFactorial.run()
    factorials.forEach { (n, fact) ->
        println("$n! = $fact")
    }
}
```

---

## Часть 3. Корутины

### Задание 7. Первая корутина
Используя `runBlocking` и `launch`, запустите 3 корутины, каждая из которых выводит своё имя 5 раз с `delay(500)`.

```kotlin
//import kotlinx.coroutines.*
//import kotlin.coroutines.CoroutineContext

object CoroutineLaunch {
    fun run(): List<String> = runBlocking {
        val results = mutableListOf<String>()
        val jobs = List(3) { index ->
            launch {
                repeat(5) { i ->
                    val message = "Корутина-$index: вывод ${i + 1}"
                    println(message)
                    results.add(message)
                    delay(500)
                }
            }
        }
        jobs.joinAll()
        return@runBlocking results
    }
}

// Тестирование
fun main() {
    println("\nПервая корутина")
    runBlocking {
        CoroutineLaunch.run()
    }
}
```

### Задание 8. async/await
Используя `async`, параллельно вычислите сумму чисел от 1 до 1_000_000, разбив на 4 части. Соберите результаты через `await()`.

```kotlin
object AsyncAwait {
    suspend fun run(): Long = coroutineScope {
        val total = 1_000_000L
        val chunkSize = total / 4

        val deferreds = List(4) { index ->
            async {
                val start = index * chunkSize + 1
                val end = if (index == 3) total else (index + 1) * chunkSize
                (start..end).sum()
            }
        }

        deferreds.awaitAll().sum()
    }
}

// Тестирование
fun main() {
    println("\nasync/await")
    runBlocking {
        val sum = AsyncAwait.run()
        println("Сумма чисел от 1 до 1_000_000 = $sum")
        
        val expected = (1L..1_000_000L).sum()
        println("Ожидаемое значение = $expected")
        println("Результат корректен: ${sum == expected}")
    }
}
```

### Задание 9. Structured concurrency
Создайте корутину, которая запускает 5 дочерних корутин. Если одна из них падает с исключением, все остальные должны отмениться.

```kotlin
object StructuredConcurrency {
    suspend fun run(failingCoroutineIndex: Int): Int = coroutineScope {
        val results = mutableListOf<Int>()
        val jobs = mutableListOf<Job>()

        try {
            for (i in 0 until 5) {
                val job = launch {
                    try {
                        println("Корутина $i начала работу")
                        if (i == failingCoroutineIndex) {
                            throw RuntimeException("Ошибка в корутине $i")
                        }
                        delay(500)
                        println("Корутина $i успешно завершена")
                        results.add(i)
                    } catch (e: Exception) {
                        println("Корутина $i поймала исключение: ${e.message}")
                        throw e
                    }
                }
                jobs.add(job)
            }

            jobs.joinAll()
        } catch (e: Exception) {
            println("Перехвачено исключение: ${e.message}")
            println("Все остальные корутины отменены")
        }

        return@coroutineScope results.size
    }
}

// Тестирование
fun main() {
    println("\nStructured concurrency")
    runBlocking {
        val completed = StructuredConcurrency.run(2)
        println("Завершено корутин: $completed")
    }
}
```

### Задание 10. withContext
Используя `withContext(Dispatchers.IO)`, прочитайте содержимое 3 файлов параллельно и объедините результаты.

```kotlin
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import java.io.File

object WithContextIO {
    suspend fun run(filePaths: List<String>): Map<String, String> = coroutineScope {
        val results = mutableMapOf<String, String>()
        val mutex = Mutex()

        filePaths.map { path ->
            async {
                val content = withContext(Dispatchers.IO) {
                    try {
                        File(path).readText()
                    } catch (e: Exception) {
                        "Ошибка чтения файла: ${e.message}"
                    }
                }

                mutex.withLock {
                    results[path] = content
                }
            }
        }.awaitAll()

        return@coroutineScope results
    }
}

// Тестирование
fun main() {
    println("\nwithContext IO")
    
    val testFiles = listOf("file1.txt", "file2.txt", "file3.txt")
    testFiles.forEachIndexed { index, filename ->
        File(filename).writeText("Содержимое файла ${index + 1}\nСтрока 2\nСтрока 3")
    }

    runBlocking {
        val results = WithContextIO.run(testFiles)
        results.forEach { (file, content) ->
            println("Файл: $file")
            println("Содержимое:\n$content")
            println("-".repeat(30))
        }
    }

    testFiles.forEach { File(it).delete() }
}
```

---

## Часть 4. Практическое задание

### Задание 11. Многопоточный загрузчик изображений

Напишите программу, которая параллельно скачивает изображения из интернета.

**Требования:**
1. Использовать корутины с `Dispatchers.IO`
2. Скачать 10 изображений с https://picsum.photos/200/300
3. Сохранить в папку `downloads/`
4. Вывести прогресс: "Downloaded 1/10", "Downloaded 2/10", ...
5. В конце вывести статистику: общее время, количество успешных/неуспешных загрузок

```kotlin
//import java.net.URL
//import java.time.Instant

data class DownloadStats(
    val totalTime: Long,
    val successful: Int,
    val failed: Int,
    val downloadedFiles: List<String>
)

object ImageDownloader {
    private val mutex = Mutex()

    suspend fun run(urls: List<String>, outputDir: String): DownloadStats = coroutineScope {
        val startTime = Instant.now().toEpochMilli()

        File(outputDir).mkdirs()

        var successful = 0
        var failed = 0
        val downloadedFiles = mutableListOf<String>()
        val total = urls.size

        val jobs = urls.mapIndexed { index, url ->
            async(Dispatchers.IO) {
                try {
                    val fileName = "image_${index + 1}.jpg"
                    val outputFile = File(outputDir, fileName)

                    URL(url).openStream().use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    mutex.withLock {
                        successful++
                        downloadedFiles.add(outputFile.absolutePath)
                        println("Загружено $successful/$total")
                    }
                } catch (e: Exception) {
                    mutex.withLock {
                        failed++
                        println("Ошибка загрузки $url: ${e.message}")
                    }
                }
            }
        }

        jobs.joinAll()

        val totalTime = Instant.now().toEpochMilli() - startTime

        DownloadStats(
            totalTime = totalTime,
            successful = successful,
            failed = failed,
            downloadedFiles = downloadedFiles
        )
    }
}

// Улучшенная версия с прогресс-баром
object ImageDownloaderWithProgress {
    private val mutex = Mutex()

    suspend fun run(urls: List<String>, outputDir: String): DownloadStats = coroutineScope {
        val startTime = System.currentTimeMillis()

        File(outputDir).mkdirs()

        var successful = 0
        var failed = 0
        val downloadedFiles = mutableListOf<String>()
        val total = urls.size

        val progressChannel = Channel<Int>()

        launch {
            var completed = 0
            for (count in progressChannel) {
                completed += count
                val percent = (completed * 100 / total)
                print("\rПрогресс: [$percent%] $completed/$total загружено")
            }
            println()
        }

        val jobs = urls.mapIndexed { index, url ->
            async(Dispatchers.IO) {
                try {
                    val fileName = "image_${System.currentTimeMillis()}_${index + 1}.jpg"
                    val outputFile = File(outputDir, fileName)

                    URL(url).openStream().use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    mutex.withLock {
                        successful++
                        downloadedFiles.add(outputFile.absolutePath)
                        progressChannel.send(1)
                    }
                } catch (e: Exception) {
                    mutex.withLock {
                        failed++
                        progressChannel.send(1)
                        println("\nОшибка загрузки $url: ${e.message}")
                    }
                }
            }
        }

        jobs.joinAll()
        progressChannel.close()

        val totalTime = System.currentTimeMillis() - startTime

        DownloadStats(
            totalTime = totalTime,
            successful = successful,
            failed = failed,
            downloadedFiles = downloadedFiles
        )
    }
}

// Тестирование
fun main() = runBlocking {
    println("Многопоточный загрузчик изображений")

    val urls = List(10) {
        "https://picsum.photos/200/300?random=${it + 1}"
    }

    val outputDir = "downloads"

    val stats = ImageDownloaderWithProgress.run(urls, outputDir)

    if (stats.downloadedFiles.isNotEmpty()) {
        println("\nСписок загруженных файлов:")
        stats.downloadedFiles.take(5).forEachIndexed { index, file ->
            println("   ${index + 1}. $file")
        }
        if (stats.downloadedFiles.size > 5) {
            println(" ... и еще ${stats.downloadedFiles.size - 5} файлов")
        }
    }
}
```

---
