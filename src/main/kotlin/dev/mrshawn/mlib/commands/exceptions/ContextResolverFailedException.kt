package dev.mrshawn.mlib.commands.exceptions

class ContextResolverFailedException(msg: String): Exception(msg) {

	constructor(): this("Failed to resolve context.")

}