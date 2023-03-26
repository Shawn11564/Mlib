package dev.mrshawn.mlib.tasks

import java.util.concurrent.Executors

class TaskChain {

	private val executor = Executors.newSingleThreadExecutor()
	private val tasks = ArrayList<Runnable>()

	fun first(task: Runnable): TaskChain {
		tasks[0] = task
		tasks[tasks.size - 1]
		return this
	}

	fun then(task: Runnable): TaskChain {
		tasks.add(task)
		return this
	}

	fun start() {
		executor.execute {
			tasks.forEach { it.run() }
		}
	}

}