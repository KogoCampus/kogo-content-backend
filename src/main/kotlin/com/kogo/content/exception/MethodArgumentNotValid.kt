package com.kogo.content.exception

data class MethodArgumentNotValidException (
    val location: String,
    val input: String,
    val details: String,
) : RuntimeException(details) {
    companion object {
        fun of(location: String, input: String): MethodArgumentNotValidException {
            return MethodArgumentNotValidException(
                location = location,
                input = input,
                details = ""
            )
        }
    }
}
