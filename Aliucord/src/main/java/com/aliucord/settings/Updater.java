/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.settings;

import static com.aliucord.updater.Updater.isAliucordOutdated;
import static com.aliucord.updater.Updater.isDiscordOutdated;
import static com.aliucord.updater.Updater.updateAliucord;
import static com.aliucord.updater.Updater.usingDexFromStorage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.updater.PluginUpdater;
import com.aliucord.utils.DimenUtils;
import com.aliucord.widgets.UpdaterPluginCard;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.lytefast.flexinput.R;

public class Updater extends SettingsPage {
    private String stateText = "No se encontró actualizaciones";

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewBound(View view) {
        super.onViewBound(view);

        setActionBarTitle("Actualizaciones");
        setActionBarSubtitle(stateText);

        var context = view.getContext();
        int padding = DimenUtils.getDefaultPadding();

        Utils.threadPool.execute(() -> {
            Snackbar sb;
            if (usingDexFromStorage()) {
                sb = Snackbar.make(getLinearLayout(), "Aliucord no se puede utilizar por que estás usandolo desde el almacenamiento", Snackbar.LENGTH_INDEFINITE);
            } else if (isDiscordOutdated()) {
                sb = Snackbar
                    .make(getLinearLayout(), "Tu Discord base está desactualizado.", BaseTransientBottomBar.LENGTH_INDEFINITE)
                    .setAction("Open Installer", v -> {
                        var ctx = v.getContext();
                        var i = ctx.getPackageManager().getLaunchIntentForPackage("com.aliucord.installer");
                        if (i != null)
                            ctx.startActivity(i);
                        else
                            Utils.showToast("Please install the Aliucord installer and try again.");
                    });
            } else if (isAliucordOutdated()) {
                sb = Snackbar
                    .make(getLinearLayout(), "Tu Aliucord está desactualizado", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Update", v -> Utils.threadPool.execute(() -> {
                        var ctx = v.getContext();
                        try {
                            updateAliucord(ctx);
                            Utils.showToast("¡Actualizado exitosamente!");
                            Snackbar rb = Snackbar
                                .make(getLinearLayout(), "Reinicia para aplicar la actualizacion.", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Restart", e -> {
                                    Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                                    context.startActivity(Intent.makeRestartActivityTask(intent.getComponent()));
                                    Runtime.getRuntime().exit(0);
                                });
                            rb.setBackgroundTint(0xffffbb33);
                            rb.setTextColor(Color.BLACK);
                            rb.setActionTextColor(Color.BLACK);
                            rb.show();
                        } catch (Throwable th) {
                            PluginUpdater.logger.errorToast("Oops. No se pudo actualizar Aliucord. Revisa el menú de depuración para más información.", th);
                        }
                    }));
            } else return;

            sb
                .setBackgroundTint(0xffffbb33) // https://developer.android.com/reference/android/R.color#holo_orange_light
                .setTextColor(Color.BLACK)
                .setActionTextColor(Color.BLACK)
                .show();
        });

        addHeaderButton("Refrescar", Utils.tintToTheme(Utils.getDrawableByAttr(context, R.b.ic_refresh)), item -> {
            item.setEnabled(false);
            setActionBarSubtitle("Buscando actualizaciones...");
            Utils.threadPool.execute(() -> {
                PluginUpdater.cache.clear();
                PluginUpdater.checkUpdates(false);
                int updateCount = PluginUpdater.updates.size();
                if (updateCount == 0)
                    stateText = "Nada para actualizar.";
                else
                    stateText = String.format("Se encontró %s", Utils.pluralise(updateCount, "actualizaci"));
                Utils.mainThread.post(this::reRender);
            });
            return true;
        });

        addHeaderButton("Actualizar todo", R.e.ic_file_download_white_24dp, item -> {
            item.setEnabled(false);
            setActionBarSubtitle("Actualizando...");
            Utils.threadPool.execute(() -> {
                int updateCount = PluginUpdater.updateAll();
                if (updateCount == 0) {
                    stateText = "Nada para actualizar.";
                } else if (updateCount == -1) {
                    stateText = "Algo falló durante el proceso. Intentalo de nuevo.";
                } else {
                    stateText = "Todos los plugins actualizados!";
                }
                Utils.mainThread.post(this::reRender);
            });
            return true;
        });

        int updateCount = PluginUpdater.updates.size();

        if (updateCount == 0) {
            TextView state = new TextView(context, null, 0, R.i.UiKit_Settings_Item_SubText);
            state.setText(stateText);
            state.setPadding(padding, padding, padding, padding);
            state.setGravity(Gravity.CENTER);
            addView(state);
            return;
        }

        stateText = "Se encontró " + Utils.pluralise(updateCount, "actualizaci");
        setActionBarSubtitle(stateText);

        for (String plugin : PluginUpdater.updates)
            addView(new UpdaterPluginCard(context, plugin, this::reRender));
    }
}
