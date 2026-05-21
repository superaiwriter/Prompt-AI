package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null) return ""
        return list.joinToString("|||")
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data == null || data.isEmpty()) return emptyList()
        return data.split("|||")
    }
}
