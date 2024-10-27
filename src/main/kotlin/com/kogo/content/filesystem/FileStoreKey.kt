package com.kogo.content.filesystem

import com.kogo.content.SpringContext
import com.kogo.content.service.filehandler.FileHandler

data class FileStoreKey (
    val key: String
) {
    override fun toString(): String = key

    fun toFileSourceUrl(): String {
        val fileHandler = SpringContext.getBean(FileHandler::class.java) as FileHandler
        return fileHandler.issueFilePublicSourceUrl(this)
    }
}
