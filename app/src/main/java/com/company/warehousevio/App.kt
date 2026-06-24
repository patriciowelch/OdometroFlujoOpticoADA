package com.company.warehousevio

import android.app.Application
import com.company.warehousevio.network.WifiDirectManager

/** Subclase Application para alojar singletons de alcance de aplicación. */
class App : Application() {
    /** Singleton de WifiDirectManager; inicializado en primer acceso. */
    val wifiDirectManager by lazy { WifiDirectManager(this) }
}
