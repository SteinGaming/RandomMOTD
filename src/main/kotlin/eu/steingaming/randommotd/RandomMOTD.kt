package eu.steingaming.randommotd

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.server.ServerLifecycleHooks
import java.io.File

@Mod(RandomMOTD.MODID)
class RandomMOTD constructor(val fml: FMLJavaModLoadingContext) {
    companion object {
        const val MODID = "randommotd"
        private val configFile: File = File("config/randommotd.json")
    }

    data class Configuration(
        val delay: Long = 5000,
        val motds: JsonArray = JsonArray().apply {
            add("§7A Minecraft Server")
            add("§4NOT a Minecraft Server\n§7(Maybe it is)")
        }
    )
    private val config: Configuration
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private var index = 0
    init {
        config = (try {
            gson.fromJson(configFile.also { it.createNewFile() }.reader(), Configuration::class.java)
        } catch (e: Exception) {
            null
        } ?: Configuration()).also { config ->
            configFile.writer().use {
                it.write(gson.toJson(config))
            }
        }
        fml.modEventBus.addListener<FMLDedicatedServerSetupEvent> {
            GlobalScope.launch {
                while (true) {
                    delay(config.delay)
                    val server = ServerLifecycleHooks.getCurrentServer() ?: let {
                        println("Server was null, waiting...")
                        null
                    } ?: continue
                    server.motd = config.motds.get(index).takeUnless {
                        it == null || it.isJsonNull
                    }?.asString ?: let {
                        index = 0
                        config.motds[0].asString
                    }
                    index = (index + 1) % config.motds.size()
                }
            }
        }
    }
}