@file:Suppress("UNCHECKED_CAST")

package models

abstract class BaseJsonObject(val rawData: Map<*, *>) {

    fun m(key: String): Map<*, *>? = rawData.m(key)

    fun <T> g(key: String): T? = rawData[key] as T?

    fun l(key: String): List<Map<*, *>> = rawData.l(key)
}

fun Map<*, *>.m(key: String): Map<*, *>? = this[key] as Map<*, *>?

fun <T> Map<*, *>.g(key: String): T? = this[key] as T?

fun Map<*, *>.l(key: String): List<Map<*, *>> = this[key] as List<Map<*, *>>? ?: listOf()