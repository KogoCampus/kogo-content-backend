package com.kogo.content.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.web.multipart.MultipartFile

class FileValidator {

    private var sizeLimit: Int = 128000000 // 128MB
    private lateinit var acceptedMediaTypes: Array<String>

    inner class Validator : ConstraintValidator<ValidFile, MultipartFile> {
        override fun initialize(constraintAnnotation: ValidFile) {
            sizeLimit = constraintAnnotation.sizeLimit
            acceptedMediaTypes = constraintAnnotation.acceptedMediaTypes
            super.initialize(constraintAnnotation)
        }

        override fun isValid(file: MultipartFile?, context: ConstraintValidatorContext?): Boolean {
            if (file == null) return true
            return validateFile(file, context)
        }
    }

    inner class ListValidator : ConstraintValidator<ValidFile, List<MultipartFile>> {
        override fun initialize(constraintAnnotation: ValidFile) {
            sizeLimit = constraintAnnotation.sizeLimit
            acceptedMediaTypes = constraintAnnotation.acceptedMediaTypes
            super.initialize(constraintAnnotation)
        }

        override fun isValid(files: List<MultipartFile>?, context: ConstraintValidatorContext?): Boolean {
            if (files.isNullOrEmpty()) return true

            for (file in files)
                if (!validateFile(file, context)) return false

            return true
        }
    }

    fun validateFile(file: MultipartFile, context: ConstraintValidatorContext?): Boolean {
        if (sizeLimit > 0 && file.size > sizeLimit) {
            context?.buildConstraintViolationWithTemplate("File size exceeds limit of ${sizeLimit / 1000000}MB")
                ?.addConstraintViolation()
            return false
        }
        if (!acceptedMediaTypes.contains(file.contentType)) {
            context?.buildConstraintViolationWithTemplate("Unsupported media type: ${file.contentType}")
                ?.addConstraintViolation()
            return false
        }
        return true;
    }
}
