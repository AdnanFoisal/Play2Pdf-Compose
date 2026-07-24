package com.adnanfoisal.play2pdf.data.db

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Room TypeConverters for List<String> columns.
 *
 * Uses Moshi with [KotlinJsonAdapterFactory] because we don't want to
 * pay the KSP codegen cost for an adapter for `List<String>` —
 * reflection-based adapter is fine here, the type is trivial.
 */
class Converters {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val stringListAdapter =
        moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        if (value == null) "[]" else stringListAdapter.toJson(value)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else stringListAdapter.fromJson(value) ?: emptyList()
}
