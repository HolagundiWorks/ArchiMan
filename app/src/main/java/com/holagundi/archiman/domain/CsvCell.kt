package com.holagundi.archiman.domain
object CsvCell {
    private val numberPattern=Regex("[+-]?[0-9]{1,15}(\\.[0-9]{1,6})?")
    fun quote(raw:String):String {
        val first=raw.trimStart { it.isWhitespace() || it=='\uFEFF' || it.code<32 }.firstOrNull()
        val numeric=raw.matches(numberPattern)
        val safe=if(first in listOf('=','+','-','@') && !numeric) "'$raw" else raw
        return "\""+safe.replace("\"","\"\"")+"\""
    }
}
