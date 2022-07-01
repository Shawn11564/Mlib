package dev.mrshawn.mlib.files

import java.io.File

class Kson(
	private val fileName: String,
	private val path: String
) {

	private val file = File("$path${File.separator}$fileName.json")

	init {
		if (!file.exists()) file.createNewFile()
	}

}