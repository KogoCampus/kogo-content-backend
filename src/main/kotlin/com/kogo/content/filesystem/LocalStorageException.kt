package com.kogo.content.filesystem

class LocalStorageException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor(message: String?) : this (message, null)
}
