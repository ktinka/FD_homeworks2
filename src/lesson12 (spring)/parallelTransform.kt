package homework

/**
 * Задание: Параллельное преобразование элементов списка с использованием async.
 *
 * Преобразуйте каждый элемент списка в отдельной корутине с помощью async.
 *
 * @param items список элементов для преобразования
 * @param transform функция преобразования
 * @return список преобразованных элементов в исходном порядке
 */

// Решение - параллельность преобразований

package homework

import kotlinx.coroutines.*

suspend fun <T, R> parallelTransform(
    items: List<T>,
    transform: suspend (T) -> R
): List<R> = coroutineScope {
    
    val deferredResults = items.map { item ->
        async(Dispatchers.Default) {
            transform(item)
        }
    }
    
    deferredResults.map { it.await() }
}
