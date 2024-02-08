package dev.mrshawn.mlib.recipes

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.java.JavaPlugin

class ShapelessRecipeBuilder {

	private lateinit var id: String
	private lateinit var result: ItemStack
	private val ingredients = mutableListOf<RecipeChoice>()

	fun id(id: String): ShapelessRecipeBuilder {
		this.id = id
		return this
	}

	fun addIngredient(material: Material): ShapelessRecipeBuilder {
		ingredients.add(RecipeChoice.MaterialChoice(material))
		return this
	}

	fun addIngredient(vararg materialChoices: Material): ShapelessRecipeBuilder {
		ingredients.add(RecipeChoice.MaterialChoice(materialChoices.toList()))
		return this
	}

	fun result(result: ItemStack): ShapelessRecipeBuilder {
		this.result = result
		return this
	}

	fun build(): ShapelessRecipe {
		val recipe = ShapelessRecipe(NamespacedKey(JavaPlugin.getProvidingPlugin(this.javaClass), id), result)
		recipe.addIngredient(RecipeChoice.MaterialChoice())
		return recipe
	}

}