package dev.mrshawn.mlib.items.builders

import dev.mrshawn.mlib.items.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

class SkullBuilder(private var owner: OfflinePlayer, amount: Int = 1): ItemBuilder(Material.PLAYER_HEAD, amount) {

	constructor(owner: UUID, amount: Int = 1): this(Bukkit.getOfflinePlayer(owner), amount)

	constructor(owner: String, amount: Int = 1): this(Bukkit.getOfflinePlayer(owner), amount)

	constructor(owner: ItemUtils.MhfHeads, amount: Int = 1): this(owner.name, amount)

	init {
		setOwner(owner)
	}

	fun setOwner(owner: OfflinePlayer): SkullBuilder {
		this.owner = owner
		(this.meta as SkullMeta).owningPlayer = owner
		return this
	}

}