package com.kogo.content.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

fun validateTag(tag: String?): Boolean {
    if (tag.isNullOrBlank()) return false
    return !tag.contains("[!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]".toRegex())
}

class TagValidator: ConstraintValidator<ValidTag, String> {

    override fun isValid(tag: String?, context: ConstraintValidatorContext?): Boolean
        = validateTag(tag)
}

class TagListValidator: ConstraintValidator<ValidTag, List<String>> {
    override fun isValid(tags: List<String>?, context: ConstraintValidatorContext?): Boolean =
        if (tags.isNullOrEmpty()) true
        else tags.all { validateTag(it) }
}
