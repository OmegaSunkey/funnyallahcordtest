/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.settings

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.aliucord.*
import com.aliucord.fragments.SettingsPage
import com.discord.stores.StoreStream
import com.discord.views.CheckedSetting
import com.lytefast.flexinput.R

// These keys aren't consistent because they were originally part of different modules
const val AUTO_DISABLE_ON_CRASH_KEY = "autoDisableCrashingPlugins"
const val AUTO_UPDATE_PLUGINS_KEY = "AC_plugins_auto_update_enabled"
const val AUTO_UPDATE_ALIUCORD_KEY = "AC_aliucord_auto_update_enabled"
const val ALIUCORD_FROM_STORAGE_KEY = "AC_from_storage"

class AliucordPage : SettingsPage() {
    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("Aliucord")
        setActionBarSubtitle("Ajustes de Aliucord")

        val ctx = view.context

        addHeader(ctx, "Ajustes de Aliucord")
        addSwitch(ctx,
            AUTO_DISABLE_ON_CRASH_KEY,
            "Desactivar automaticamente complementos con errores fatales",
            "Cuando un complemento detenga Aliucord abruptamente, será desactivado
            automaticamente.",
            true
        )
        //addSwitch(ctx, AUTO_UPDATE_ALIUCORD_KEY, "Actualizar Aliucord automaticamente", null)
        addSwitch(ctx, AUTO_UPDATE_PLUGINS_KEY, "Actualizar plugins automaticamente", null)

        if (StoreStream.getUserSettings().isDeveloperMode) {
            addDivider(ctx)
            addHeader(ctx, "Ajustes de desarrollador")
            addSwitch(
                ctx,
                ALIUCORD_FROM_STORAGE_KEY,
                "Usar Aliucord desde el almacenamiento principal",
                "Esta función es principalmente para desarrolladores, asi que no la actives a menos que sepas lo que haces. Si alguien
                te dice que hagas esto, probablemente te esten engañando."
            )
        }

        addDivider(ctx)
        addHeader(ctx, "Enlaces")
        addLink(ctx, "Código fuente", R.e.ic_account_github_white_24dp) {
            Utils.launchUrl(Constants.ALIUCORD_GITHUB_REPO)
        }
        addLink(ctx, "Servidor de soporte", R.e.ic_help_24dp) {
            Utils.joinSupportServer(it.context)
        }
        addLink(ctx, "Apoyanos en GitHub Sponsors", R.e.ic_heart_24dp) {
            Utils.launchUrl("https://github.com/sponsors/Juby210")
        }
    }

    private fun addSwitch(ctx: Context, setting: String, title: String, subtitle: String?, default: Boolean = false) {
        Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, title, subtitle).run {
            isChecked = Main.settings.getBool(setting, default)
            setOnCheckedListener {
                Main.settings.setBool(setting, it)
            }
            linearLayout.addView(this)
        }
    }

    private fun addLink(ctx: Context, text: String, @DrawableRes drawable: Int, action: View.OnClickListener) {
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon).run {
            this.text = text
            val drawableEnd = ContextCompat.getDrawable(ctx, R.e.ic_open_in_new_white_24dp)?.run {
                mutate()
                Utils.tintToTheme(this)
            }
            val drawableStart = ContextCompat.getDrawable(ctx, drawable)?.run {
                mutate()
                Utils.tintToTheme(this)
            }
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawableStart, null, drawableEnd, null)
            setOnClickListener(action)
            linearLayout.addView(this)
        }
    }
}
