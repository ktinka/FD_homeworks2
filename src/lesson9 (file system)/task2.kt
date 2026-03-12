package `lesson9 (file system)`

/**
 * ## Задача: Создать собственный архиватор.
 * Напишите функцию, которая:

1. Принимает на вход **каталог с файлами** (например, `project_data/`) и путь к архиву (`archive.zip`).
2. Создает **ZIP-архив** всех файлов внутри каталога, сохраняя структуру подкаталогов.
3. Использует классы `FileInputStream`, `FileOutputStream` и `java.util.zip.ZipOutputStream`.
4. Обеспечивает корректное закрытие потоков и обработку исключений.
5. Добавить фильтр по расширению файлов, чтобы архивировать только `.txt` или `.log`.

### Требования:
- Не использовать сторонние библиотеки для архивирования (только стандартный API).
- Программа должна выводить в консоль список добавляемых файлов и их размер.
 **/

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun createArchive(
    sourceDir: String,
    archivePath: String,
    extensions: List<String> = listOf(".txt", ".log")
) {
    val sourceDirectory = File(sourceDir)

    if (!sourceDirectory.exists() || !sourceDirectory.isDirectory) {
        println("Error! Каталог '$sourceDir' не существует или не является директорией")
        return
    }

    val archiveFile = File(archivePath)
    archiveFile.parentFile?.mkdirs()

    FileOutputStream(archivePath).use { fos ->
        ZipOutputStream(fos).use { zos ->
            addFilesToZip(sourceDirectory, sourceDirectory, zos, extensions)
        }
    }

    println("\nАрхив успешно создан. Архив - $archivePath")
    println("Размер архива: ${archiveFile.length()} байт")
}

private fun addFilesToZip(
    rootDir: File,
    currentDir: File,
    zos: ZipOutputStream,
    extensions: List<String>
) {
    currentDir.listFiles()?.forEach { file ->
        when {
            file.isFile -> {
                if (extensions.isEmpty() || extensions.any { file.name.endsWith(it, ignoreCase = true) }) {
                    addFileToZip(file, rootDir, zos)
                } else {
                    println("Пропущен (неподходящее расширение) ${file.path}")
                }
            }
            file.isDirectory -> {
                addFilesToZip(rootDir, file, zos, extensions)
            }
        }
    }
}

private fun addFileToZip(file: File, rootDir: File, zos: ZipOutputStream) {
    try {
        val relativePath = file.toRelativeString(rootDir)
        val zipEntry = ZipEntry(relativePath)
        zos.putNextEntry(zipEntry)

        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytes = 0L

            while (fis.read(buffer).also { bytesRead = it } > 0) {
                zos.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }

            println("Добавлено $relativePath (${totalBytes} байт)")
        }

        zos.closeEntry()
    } catch (e: Exception) {
        println("Error! Ошибка при добавлении файла '${file.path}': ${e.message}")
    }
}


fun main() {
    println("Тестирование архиватора :з")
    try {
        createArchive(
            sourceDir = "project_data",
            archivePath = "archive.zip"
        )
    } catch (e: Exception) {
        println("Error! Ошибка: ${e.message}")
    }
}
