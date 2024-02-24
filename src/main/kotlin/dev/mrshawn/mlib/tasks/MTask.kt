package dev.mrshawn.mlib.tasks

import kotlin.random.Random

abstract class MTask {

	val taskID = Random.nextInt(0, 1000000)

	abstract fun run()

}