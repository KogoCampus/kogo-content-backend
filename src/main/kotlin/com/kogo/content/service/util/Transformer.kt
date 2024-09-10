package com.kogo.content.service.util

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

open class Transformer<in IN : Any, out OUT : Any>
protected constructor(inClass: KClass<IN>, outClass: KClass<OUT>)
{
    private val outConstructor = outClass.primaryConstructor!!
    private val inPropertiesByName by lazy {
        inClass.memberProperties.associateBy { it.name }
    }

    fun transform(data: IN): OUT = with(outConstructor) {
        callBy(parameters.associateWith { parameter -> argFor(parameter, data) })
    }

    /**
     * If an object requires a custom property name mapping, override this function.
     *     override fun argFor(parameter: KParameter, data: PersonForm): Any? {
     *         return when (parameter.name) {
     *             "name" -> with(data) { "$firstName $lastName" }
     *             else -> super.argFor(parameter, data)
     *         }
     *     }
     */
    open fun argFor(parameter: KParameter, data: IN): Any? {
        return inPropertiesByName[parameter.name]?.get(data)
    }
}
