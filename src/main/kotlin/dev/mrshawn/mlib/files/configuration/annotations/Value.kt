package dev.mrshawn.mlib.files.configuration.annotations

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Value(val path: String, val comments: Array<String> = [])