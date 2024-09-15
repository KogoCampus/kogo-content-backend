package com.kogo.content.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.web.multipart.MultipartFile

fun validateFile(file: MultipartFile, sizeMax: Int, sizeMin: Int, acceptedMediaTypes: Array<String>, context: ConstraintValidatorContext?): Boolean {
    if (sizeMax > 0 && file.size > sizeMax) {
        context?.buildConstraintViolationWithTemplate("File size exceeds limit of ${sizeMax / 1000000}MB")
            ?.addConstraintViolation()
        return false
    }
    else if (sizeMin > 0 && sizeMin > file.size) {
        context?.buildConstraintViolationWithTemplate("File size smaller than minimum limit of ${sizeMin / 1000000}MB")
            ?.addConstraintViolation()
        return false
    }
    if (acceptedMediaTypes.isNotEmpty() && !acceptedMediaTypes.contains(file.contentType)) {
        context?.buildConstraintViolationWithTemplate("Unsupported media type: ${file.contentType}")
            ?.addConstraintViolation()
        return false
    }
    return true;
}

class FileValidator : ConstraintValidator<ValidFile, MultipartFile> {

    private var sizeMax: Int = 0
    private var sizeMin: Int = 0
    private var acceptedMediaTypes: Array<String> = arrayOf()

    override fun initialize(constraintAnnotation: ValidFile) {
        sizeMax = constraintAnnotation.sizeMax
        sizeMin = constraintAnnotation.sizeMin
        acceptedMediaTypes = constraintAnnotation.acceptedMediaTypes
        super.initialize(constraintAnnotation)
    }

    override fun isValid(file: MultipartFile?, context: ConstraintValidatorContext?): Boolean {
        if (file == null) return true
        return validateFile(
            file = file,
            sizeMax = sizeMax,
            sizeMin = sizeMin,
            acceptedMediaTypes = acceptedMediaTypes,
            context = context
        )
    }
}

class FileListValidator : ConstraintValidator<ValidFile, List<MultipartFile>?> {

    private var sizeMax: Int = 0
    private var sizeMin: Int = 0
    private var acceptedMediaTypes: Array<String> = arrayOf()

    override fun initialize(constraintAnnotation: ValidFile) {
        sizeMax = constraintAnnotation.sizeMax
        sizeMin = constraintAnnotation.sizeMin
        acceptedMediaTypes = constraintAnnotation.acceptedMediaTypes
        super.initialize(constraintAnnotation)
    }

    override fun isValid(files: List<MultipartFile>?, context: ConstraintValidatorContext?): Boolean {
        if (files.isNullOrEmpty()) return true
        return files.all {
            validateFile(
                file = it,
                sizeMax = sizeMax,
                sizeMin = sizeMin,
                acceptedMediaTypes = acceptedMediaTypes,
                context = context
            )
        }
    }
}
