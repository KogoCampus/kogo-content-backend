package com.kogo.content.exception

data class UnsupportedMediaTypeException(
    val details: String? = "",
    val mediaType: String,
    val acceptedMediaType: String,
) : RuntimeException(details) {
    companion object {
        fun of(mediaType: String, acceptedMediaType: String): UnsupportedMediaTypeException {
            return UnsupportedMediaTypeException(
                mediaType = mediaType,
                acceptedMediaType = acceptedMediaType,
                details = "$mediaType is not supported. Please upload $acceptedMediaType."
            )
        }
    }
}
