package dev.mrshawn.mlib.commands.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class CommandCompletion(val completions: String)