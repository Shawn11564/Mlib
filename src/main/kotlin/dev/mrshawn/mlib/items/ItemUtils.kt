package dev.mrshawn.mlib.items

import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType

object ItemUtils {

	fun getItemID(item: ItemStack): String? {
		val meta = item.itemMeta ?: return null
		val dataContainer = meta.persistentDataContainer

		return dataContainer.get(GuiItem.KEY_UUID, PersistentDataType.STRING)
	}

	fun getMhfHead(head: MhfHeads): ItemStack {
		val item = ItemStack(Material.PLAYER_HEAD)
		item.setItemMeta(getMhfSkullMeta(head))

		return item
	}

	fun getMhfSkullMeta(head: MhfHeads): SkullMeta {
		val meta = Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD) as SkullMeta
		meta.owner = head.playerName

		return meta
	}

	enum class MhfHeads(val playerName: String) {
		MHF_ALEX("MHF_Alex"),
		MHF_BLAZE("MHF_Blaze"),
		MHF_CAVESPIDER("MHF_CaveSpider"),
		MHF_CHICKEN("MHF_Chicken"),
		MHF_COW("MHF_Cow"),
		MHF_CREEPER("MHF_Creeper"),
		MHF_ENDERMAN("MHF_Enderman"),
		MHF_GHAST("MHF_Ghast"),
		MHF_GOLEM("MHF_Golem"),
		MHF_HEROBRINE("MHF_Herobrine"),
		MHF_LAVASLIME("MHF_LavaSlime"),
		MHF_MUSHROOMCOW("MHF_MushroomCow"),
		MHF_OCELOT("MHF_Ocelot"),
		MHF_PIG("MHF_Pig"),
		MHF_PIGZOMBIE("MHF_PigZombie"),
		MHF_SHEEP("MHF_Sheep"),
		MHF_SKELETON("MHF_Skeleton"),
		MHF_SLIME("MHF_Slime"),
		MHF_SPIDER("MHF_Spider"),
		MHF_SQUID("MHF_Squid"),
		MHF_STEVE("MHF_Steve"),
		MHF_VILLAGER("MHF_Villager"),
		MHF_WSKELETON("MHF_WSkeleton"),
		MHF_ZOMBIE("MHF_Zombie"),
		MHF_CACTUS("MHF_Cactus"),
		MHF_CAKE("MHF_Cake"),
		MHF_CHEST("MHF_Chest"),
		MHF_COCONUTB("MHF_CoconutB"),
		MHF_COCONUTG("MHF_CoconutG"),
		MHF_MELON("MHF_Melon"),
		MHF_OAKLOG("MHF_OakLog"),
		MHF_PRESENT1("MHF_Present1"),
		MHF_PRESENT2("MHF_Present2"),
		MHF_PUMPKIN("MHF_Pumpkin"),
		MHF_TNT("MHF_TNT"),
		MHF_TNT2("MHF_TNT2"),
		MHF_ARROWUP("MHF_ArrowUp"),
		MHF_ARROWDOWN("MHF_ArrowDown"),
		MHF_ARROWLEFT("MHF_ArrowLeft"),
		MHF_ARROWRIGHT("MHF_ArrowRight"),
		MHF_EXCLAMATION("MHF_Exclamation"),
		MHF_QUESTION("MHF_Question");
	}

}