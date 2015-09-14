package org.jetbrains.ktor.locations

import org.jetbrains.ktor.routing.*
import java.lang
import java.lang.reflect.*

public interface ConversionService {
    fun fromContext(context: RoutingApplicationRequestContext, name: String, type: Type, optional: Boolean): Any?
    fun toURI(value: Any?, name: String, optional: Boolean): List<String>
}

public open class DefaultConversionService : ConversionService {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun toURI(value: Any?, name: String, optional: Boolean): List<String> {
        return when (value) {
            null -> listOf<String>()
            is Iterable<*> -> value.flatMap { toURI(it, name, optional) }
            else -> {
                val type = value.javaClass
                listOf(when (type) {
                           Int::class.java, lang.Integer::class.java,
                           Float::class.java, lang.Float::class.java,
                           Double::class.java, lang.Double::class.java,
                           Long::class.java, lang.Long::class.java,
                           Boolean::class.java, lang.Boolean::class.java,
                           String::class.java, lang.String::class.java -> value.toString()
                           else -> throw UnsupportedOperationException("Type $type is not supported in automatic location data class processing")
                       })
            }
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    open fun convert(value: String, type: Type): Any {
        return when (type) {
            is WildcardType -> convert(value, type.upperBounds.single())
            Int::class.java, lang.Integer::class.java -> value.toInt()
            Float::class.java, lang.Float::class.java -> value.toFloat()
            Double::class.java, lang.Double::class.java -> value.toDouble()
            Long::class.java, lang.Long::class.java -> value.toLong()
            Boolean::class.java, lang.Boolean::class.java -> value.toBoolean()
            String::class.java, lang.String::class.java -> value
            else -> throw UnsupportedOperationException("Type $type is not supported in automatic location data class processing")
        }
    }

    open fun convert(values: List<String>, type: Type): Any {
        if (type is ParameterizedType) {
            val rawType = type.rawType as Class<*>
            if (rawType.isAssignableFrom(List::class.java)) {
                val itemType = type.actualTypeArguments.single()
                return values.map { convert(it, itemType) }
            }
        }

        if (values.size() != 1) {
            throw InconsistentRoutingException("There are multiply values in request when trying to construct single value $type")
        }

        return convert(values.single(), type)
    }

    override fun fromContext(context: RoutingApplicationRequestContext, name: String, type: Type, optional: Boolean): Any? {
        val requestParameters = context.parameters.getAll(name)
        return if (requestParameters == null) {
            if (!optional) {
                throw InconsistentRoutingException("Parameter '$name' was not found in the request")
            }
            null
        } else {
            convert(requestParameters, type)
        }
    }
}