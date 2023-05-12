package me.melijn.apredgres

public data class ExposedIndex(
    val unique: Boolean,
    val fields: List<String>,
    val name: String? = null,
)