package com.company.warehousevio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.company.warehousevio.App
import com.company.warehousevio.ui.theme.WarehouseVIOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WarehouseVIOTheme {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Registrar receptor Wi-Fi Direct para detectar peers y cambios de conexión
        (application as App).wifiDirectManager.register()
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar receptor al pausar para evitar leaks
        (application as App).wifiDirectManager.unregister()
    }
}
