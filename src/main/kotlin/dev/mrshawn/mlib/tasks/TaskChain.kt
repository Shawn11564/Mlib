package dev.mrshawn.mlib.tasks

import java.util.concurrent.Executors

class TaskChain {

	private val executor = Executors.newSingleThreadExecutor()
	private val tasks = ArrayList<MTask>()

	fun first(task: MTask): TaskChain {
		tasks[0] = task
		tasks[tasks.size - 1]
		return this
	}

	fun then(task: MTask): TaskChain {
		tasks.add(task)
		return this
	}

	fun start() {
		executor.execute {
			tasks.forEach { it.run() }
		}
	}

}