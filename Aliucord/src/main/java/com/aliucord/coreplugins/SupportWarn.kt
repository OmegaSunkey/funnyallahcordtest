package com.aliucord.coreplugins

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.aliucord.Constants.PLUGIN_REQUESTS_CHANNEL_ID
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.CorePlugin
import com.aliucord.fragments.InputDialog
import com.aliucord.patcher.*
import com.aliucord.settings.delegate
import com.discord.widgets.chat.input.ChatInputViewModel
import com.discord.widgets.chat.input.WidgetChatInput
import com.lytefast.flexinput.R

@SuppressLint("SetTextI18n")
internal class SupportWarn : CorePlugin(Manifest("SupportWarn")) {
    private val SettingsAPI.acceptedPrdNotRequests: Boolean by settings.delegate(false)
    private val SettingsAPI.acceptedDevNotSupport: Boolean by settings.delegate(false)

    init {
        manifest.description = "Show a warning prior to interacting with the Aliucord server"
    }

    // Allow this to be disabled once warning has been acknowledged
    override val isRequired get() = !settings.acceptedDevNotSupport

    override fun start(context: Context) {
        if (settings.acceptedPrdNotRequests && settings.acceptedDevNotSupport) return

        val channelList = listOf(
            811255667469582420L, // #offtopic
            811261478875299840L, // #plugin-development
            868419532992172073L, // #theme-development
            865188789542060063L, // #related-development
            811262084968742932L, // #core-development
            PLUGIN_REQUESTS_CHANNEL_ID,
        )

        val chatWrapId = Utils.getResId("chat_input_wrap", "id")
        val gateButtonTextId = Utils.getResId("chat_input_member_verification_guard_text", "id")
        val gateButtonImageId = Utils.getResId("chat_input_member_verification_guard_icon", "id")
        val gateButtonArrowId = Utils.getResId("chat_input_member_verification_guard_action", "id")
        val gateButtonLayoutId = Utils.getResId("guard_member_verification", "id")

        patcher.after<WidgetChatInput>("configureChatGuard", ChatInputViewModel.ViewState.Loaded::class.java)
        { (_, loaded: ChatInputViewModel.ViewState.Loaded) ->
            if (loaded.channelId !in channelList || loaded.shouldShowVerificationGate) return@after

            val (text, desc, key) = if (loaded.channelId == PLUGIN_REQUESTS_CHANNEL_ID) {
                if (settings.acceptedPrdNotRequests) return@after
                Triple(
                    " LEASE: Este canal no sirve para peticiones de plugins, no intente hacerlo!",
                    "ESTE CANAL NO RECIBE PETICIONES. Para más información sobre como recomendar un plugin, mire los mensajes fijados en este canal. Si leíste esto, escribe \"Entiendo.\" en la caja de texto.",
                    "acceptedPrdNotRequests"
                )
            } else {
                if (settings.acceptedDevNotSupport) return@after
                Triple(
                    "LEASE: Este canal no es de soporte técnico, asi que no pregunte por ayuda aquí.",
                    "ESTE CANAL NO ES PARA AYUDAR. No pregunte por ayuda sobre instalación de plugins o de temas o serás silenciado aquí. Si leíste esto, escribe \"Entiendo.\"",
                    "acceptedDevNotSupport"
                )
            }

            val root = WidgetChatInput.`access$getBinding$p`(this).root
            val gateButtonLayout = root.findViewById<ViewGroup>(gateButtonLayoutId)
            val chatWrap = root.findViewById<LinearLayout>(chatWrapId)

            gateButtonLayout.visibility = View.VISIBLE
            chatWrap.visibility = View.GONE

            root.findViewById<TextView>(gateButtonTextId).text = text
            root.findViewById<ImageView>(gateButtonImageId).setImageResource(R.e.ic_warning_circle_24dp)
            root.findViewById<ImageView>(gateButtonArrowId).setOnClickListener {
                val dialog = InputDialog()
                    .setTitle("Warning")
                    .setDescription(desc)

                dialog.setOnOkListener {
                    if (!dialog.input.contains("Entiendo.", ignoreCase = true)) return@setOnOkListener
                    settings.setBool(key, true)

                    gateButtonLayout.visibility = View.GONE
                    chatWrap.visibility = View.VISIBLE

                    dialog.dismiss()
                }

                dialog.show(this.parentFragmentManager, "Warning")
            }
        }
    }

    override fun stop(context: Context) = patcher.unpatchAll()
}
