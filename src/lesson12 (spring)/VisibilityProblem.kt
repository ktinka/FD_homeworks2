/**
 *
 * Проблема:
 * При оптимизации компилятор и процессор могут переупорядочивать операции
 * или кешировать переменные в регистрах процессора. Это приводит к тому,
 * что изменения переменной в одном потоке могут быть не видны в другом потоке.
 *
 */


// Решение - с помощью @Volatile

import kotlin.concurrent.volatile

class VisibilityProblem {

    @Volatile
    private var running = true

    fun startWriter(): Thread {
        return Thread {
            repeat(100) {
                Thread.sleep(10)
                Thread.yield()
            }

            running = false
            println("Writer: установил running = false (изменение видно всем потокам)")
        }
    }
    
    fun startReader(): Thread {
        return Thread {
            println("Reader: начал работу (ждет running = false)")

            while (running) {
                Thread.sleep(1)
            }

            println("Reader: завершил работу (увидел running = false)")
        }
    }
}
