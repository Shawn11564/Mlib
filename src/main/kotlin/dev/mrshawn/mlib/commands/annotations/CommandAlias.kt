package dev.mrshawn.mlib.commands.annotations

@Target(AnnotationTarget.CLASS)
annotation class CommandAlias(val aliases: String)
