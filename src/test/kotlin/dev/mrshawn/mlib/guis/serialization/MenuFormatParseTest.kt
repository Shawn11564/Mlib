package dev.mrshawn.mlib.guis.serialization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Deterministic, Bukkit-free tests for the wire-format mapping (Gson DTOs + the custom condition
 * deserializer). The Bukkit-touching layers (ItemMapper, DataMenu, action execution) are verified
 * in-game via the test-plugin's `/menutest`.
 */
class MenuFormatParseTest {

    private fun parse(json: String): MenuProjectDTO =
        MenuGson.GSON.fromJson(json, MenuProjectDTO::class.java)

    @Test
    fun `parses a project with every item kind, actions and panes`() {
        val json = """
        {
          "formatVersion": 1,
          "project": "demo",
          "menus": {
            "main": {
              "title": "&8Main",
              "rows": 3,
              "nextMenu": "list",
              "fill": { "item": { "material": "BLACK_STAINED_GLASS_PANE", "name": " " } },
              "items": [
                { "x": 4, "y": 1, "kind": "basic",
                  "item": { "material": "COMPASS", "name": "&aGo", "lore": ["a","b"], "amount": 2,
                            "enchantments": [{ "type": "unbreaking", "level": 3 }], "glow": true,
                            "itemFlags": ["HIDE_ATTRIBUTES"] },
                  "actions": { "requirements": ["permission:menu.use", { "any": ["op","permission:x"] }],
                               "left": [ { "type": "OPEN_MENU", "menu": "list" },
                                         { "type": "PLAY_SOUND", "sound": "UI_BUTTON_CLICK", "volume": 0.5, "pitch": 1.2 } ],
                               "right": [ { "type": "RUN_COMMAND", "as": "CONSOLE", "command": "say hi" } ] } },
                { "x": 2, "y": 1, "kind": "toggle", "initiallyToggledOn": false,
                  "on":  { "item": { "material": "LIME_DYE" }, "actions": { "left": [ { "type": "MESSAGE", "text": "on" } ] } },
                  "off": { "item": { "material": "GRAY_DYE" } } },
                { "x": 6, "y": 1, "kind": "twoStage", "glowOnFirstClick": true,
                  "item": { "material": "TNT" },
                  "secondClickActions": { "default": [ { "type": "CUSTOM", "id": "confirm", "data": { "k": "v", "n": 5 } } ] } }
              ]
            },
            "list": {
              "title": "&8List", "rows": 6, "previousMenu": "main",
              "panes": [ { "kind": "paginated", "x": 1, "y": 1, "width": 7, "height": 3,
                           "contents": [ { "item": { "material": "PAPER" } } ],
                           "navigation": { "pageIndicator": { "x": 4, "y": 5 } } } ]
            }
          }
        }
        """.trimIndent()

        val project = parse(json)
        assertEquals(1, project.formatVersion)
        assertEquals("demo", project.project)

        val menus = project.menus!!
        assertEquals(setOf("main", "list"), menus.keys)

        val main = menus["main"]!!
        assertEquals("&8Main", main.title)
        assertEquals(3, main.rows)
        assertEquals("list", main.nextMenu)
        assertEquals("BLACK_STAINED_GLASS_PANE", main.fill!!.item!!.material)

        val items = main.items!!
        assertEquals(3, items.size)

        // basic + appearance + actions
        val basic = items[0]
        assertEquals("basic", basic.kind)
        assertEquals(4, basic.x)
        val app = basic.item!!
        assertEquals("COMPASS", app.material)
        assertEquals(2, app.amount)
        assertEquals(listOf("a", "b"), app.lore)
        assertEquals(true, app.glow)
        assertEquals("unbreaking", app.enchantments!![0].type)
        assertEquals(3, app.enchantments!![0].level)
        assertEquals(listOf("HIDE_ATTRIBUTES"), app.itemFlags)

        val actions = basic.actions!!
        // requirements: string shorthand + nested object
        assertEquals(2, actions.requirements!!.size)
        assertEquals("permission", actions.requirements!![0].type)
        assertEquals("menu.use", actions.requirements!![0].value)
        assertEquals("any", actions.requirements!![1].type)
        assertEquals("op", actions.requirements!![1].children[0].type)
        // branches
        assertEquals("OPEN_MENU", actions.left!![0].type)
        assertEquals("list", actions.left!![0].menu)
        assertEquals("PLAY_SOUND", actions.left!![1].type)
        assertEquals(0.5f, actions.left!![1].volume!!)
        assertEquals("RUN_COMMAND", actions.right!![0].type)
        assertEquals("CONSOLE", actions.right!![0].runAs)
        assertEquals("say hi", actions.right!![0].command)

        // toggle with nested on/off item nodes
        val toggle = items[1]
        assertEquals("toggle", toggle.kind)
        assertEquals(false, toggle.initiallyToggledOn)
        assertEquals("LIME_DYE", toggle.on!!.item!!.material)
        assertEquals("MESSAGE", toggle.on!!.actions!!.left!![0].type)
        assertEquals("GRAY_DYE", toggle.off!!.item!!.material)

        // twoStage + custom action data (Gson numbers arrive as Double)
        val two = items[2]
        assertEquals("twoStage", two.kind)
        assertEquals(true, two.glowOnFirstClick)
        val custom = two.secondClickActions!!.default!![0]
        assertEquals("CUSTOM", custom.type)
        assertEquals("confirm", custom.id)
        assertEquals("v", custom.data!!["k"])
        assertEquals(5.0, custom.data!!["n"])

        // pane
        val list = menus["list"]!!
        assertEquals("main", list.previousMenu)
        val pane = list.panes!![0]
        assertEquals("paginated", pane.kind)
        assertEquals(7, pane.width)
        assertEquals(3, pane.height)
        assertEquals(1, pane.contents!!.size)
        assertEquals(4, pane.navigation!!.pageIndicator!!.x)
    }

    @Test
    fun `condition shorthands and object forms parse`() {
        val gamemode = MenuGson.GSON.fromJson("\"gamemode:CREATIVE\"", ConditionDTO::class.java)
        assertEquals("gamemode", gamemode.type)
        assertEquals("CREATIVE", gamemode.value)

        val op = MenuGson.GSON.fromJson("\"op\"", ConditionDTO::class.java)
        assertEquals("op", op.type)

        val not = MenuGson.GSON.fromJson("{ \"not\": \"world:nether\" }", ConditionDTO::class.java)
        assertEquals("not", not.type)
        assertNotNull(not.child)
        assertEquals("world", not.child!!.type)
        assertEquals("nether", not.child!!.value)
    }

    @Test
    fun `bare single menu has no menus map`() {
        // A document without a 'menus' key is a bare menu; the project mapper leaves menus null here
        // (MenuLoader wraps it into a one-entry project — covered by the in-game test).
        val project = parse("{ \"formatVersion\": 1, \"title\": \"&8Bare\", \"rows\": 1 }")
        assertEquals(1, project.formatVersion)
        assertEquals(null, project.menus)
    }
}
