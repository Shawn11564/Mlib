package dev.mrshawn.mlib.recipes

import org.bukkit.Bukkit
import org.bukkit.inventory.Recipe
import org.bukkit.plugin.java.JavaPlugin

object RecipeUtils {

	fun registerRecipe(recipe: Recipe) {
		Bukkit.addRecipe(recipe)
	}

	fun registerRecipes(vararg recipes: Recipe) {
		recipes.forEach { registerRecipe(it) }
	}

}