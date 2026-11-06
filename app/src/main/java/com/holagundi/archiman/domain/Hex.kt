package com.holagundi.archiman.domain
/** Allocation-bounded hex encoding for imports and archive integrity checks. */
object Hex {
    fun encode(bytes:ByteArray):String {
        val digits="0123456789abcdef";val result=CharArray(bytes.size*2)
        bytes.forEachIndexed { index, byte -> val value=byte.toInt() and 255;result[index*2]=digits[value ushr 4];result[index*2+1]=digits[value and 15] }
        return String(result)
    }
}
