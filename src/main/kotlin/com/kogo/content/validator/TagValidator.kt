package com.kogo.content.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class TagValidator : ConstraintValidator<ValidTag, String> {
    override fun isValid(value: String, context: ConstraintValidatorContext?): Boolean {
        val list = value.split(",")
        for (tag in list) {
            if (tag.contains("[!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]".toRegex())) {
                return false
            }
        }
        return true
    }
}
