package com.kogo.content.filehandler

import com.kogo.content.SpringContext

data class FileStoreKey (
    val key: String
) {
    override fun toString(): String = key

    fun toFileSourceUrl(): String {
        val fileHandler = SpringContext.getBean(FileHandler::class.java) as FileHandler
        return fileHandler.issueFilePublicSourceUrl(this)
    }
}
