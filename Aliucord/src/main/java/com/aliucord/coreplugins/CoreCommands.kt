/*
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */
package com.aliucord.coreplugins

import android.content.Context
import android.os.Build
import com.aliucord.*
import com.aliucord.api.CommandsAPI
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.CorePlugin
import com.aliucord.entities.Plugin
import com.discord.api.commands.ApplicationCommandType
import java.io.File

internal class CoreCommands : CorePlugin(Manifest("CoreCommands")) {
    init {
        manifest.description = "Adds basic slash commands to Aliucord for debugging purposes"
    }

    private fun visiblePlugins(): Sequence<Plugin> {
        return PluginManager.plugins
            .values.asSequence()
            .filter { it !is CorePlugin || !it.isHidden }
    }

    override fun start(context: Context) {
        commands.registerCommand(
            "echo",
            "Crea un mensaje hecho por Clyde",
            CommandsAPI.requiredMessageOption
        ) {
            CommandResult(it.getRequiredString("message"), null, false)
        }

        commands.registerCommand(
            "plugins",
            "Muestra los plugins instalados",
            listOf(
                Utils.createCommandOption(
                    type = ApplicationCommandType.BOOLEAN,
                    name = "enviar",
                    description = "Permite enviar la lista de plugins",
                ),
                Utils.createCommandOption(
                    type = ApplicationCommandType.BOOLEAN,
                    name = "versiones",
                    description = "Permite mostrar las versiones de los plugins",
                )
            )
        ) {
            val showVersions = it.getBoolOrDefault("versions", false)
            val (enabled, disabled) = visiblePlugins().partition(PluginManager::isPluginEnabled)

            fun formatPlugins(plugins: List<Plugin>): String =
                plugins.joinToString { p -> if (showVersions && p !is CorePlugin) "${p.name} (${p.manifest.version})" else p.name }

            if (enabled.isEmpty() && disabled.isEmpty())
                CommandResult("Todo limpio", null, false)
            else
                CommandResult(
                    """
**Plugins activados (${enabled.size}):**
${if (enabled.isEmpty()) "0" else "> ${formatPlugins(enabled)}"}
**Plugins desactivados (${disabled.size}):**
${if (disabled.isEmpty()) "0" else "> ${formatPlugins(disabled)}"}
                """,
                    null,
                    it.getBoolOrDefault("send", false)
                )
        }

        commands.registerCommand("debug", "Muestra información de depuración") {
            val customPluginCount = PluginManager.plugins.values.count { it !is CorePlugin }
            val enabledPluginCount = visiblePlugins().count(PluginManager::isPluginEnabled)

            // .trimIndent() is broken sadly due to collision with Discord's Kotlin
            var str = """
**Información de depuración:**
> Discord: ${Constants.DISCORD_VERSION}
> Aliucord: ${BuildConfig.VERSION} ${if (BuildConfig.RELEASE) "" else "(Custom)"}
> Plugins: $customPluginCount installed, $enabledPluginCount total enabled
> Sistema: Android ${Build.VERSION.RELEASE} (SDK v${Build.VERSION.SDK_INT}) - ${getArchitecture()}
> Root: ${getIsRooted() ?: "Sin root"}
            """

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val manifest = File("/apex/com.android.art/apex_manifest.pb").takeIf { it.exists() }
                    ?.readBytes()

                str += "> Versión del manifiesto de ART: ${manifest?.let { ProtobufParser.getField2(it) } ?: "Desconocido"}"
            }

            CommandResult(str)
        }
    }

    private fun getIsRooted() =
        System.getenv("PATH")?.split(':')?.any {
            File(it, "su").exists()
        }

    private fun getArchitecture(): String {
        Build.SUPPORTED_ABIS.forEach {
            when (it) {
                "arm64-v8a" -> return "aarch64"
                "armeabi-v7a" -> return "arm"
                "x86_64" -> return "x86_64"
                "x86" -> return "i686"
            }
        }
        return System.getProperty("os.arch")
            ?: System.getProperty("ro.product.cpu.abi")
            ?: "Arquitectura desconocida"
    }

    override fun stop(context: Context) {
        commands.unregisterAll()
    }
}

private object ProtobufParser {
    private fun parseVarInt(data: ByteArray, offset: Int): Pair<Long, Int> {
        var result = 0L
        var pos = offset
        var shift = 0

        while (true) {
            val byte = data[pos++].toInt() and 0xFF
            result = result or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) break
            shift += 7
        }

        return result to pos
    }

    fun getField2(data: ByteArray): Long? {
        var offset = 0

        while (offset < data.size) {
            val tag = data[offset++].toInt() and 0xFF
            if (tag shr 3 == 2) return parseVarInt(data, offset).first
            offset = when (tag and 0x07) {
                0 -> parseVarInt(data, offset).second
                1 -> offset + 8
                2 -> parseVarInt(data, offset).let { (len, off) -> off + len.toInt() }
                else -> return null
            }
        }

        return null
    }
}
