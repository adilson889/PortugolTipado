package co.adilson889.portugoltipado.apk

import android.content.Context
import co.adilson889.typec.graficos.GraficosCanvas
import co.adilson889.typec.interfaceui.Interface
import co.adilson889.typec.interfaceui.InterfaceAndroid

/**
 * Único sítio do modelo que conhece o construtor da InterfaceAndroid
 * (hoje sem argumentos). Se mudar, ajusta só a última linha.
 */
internal fun criarInterface(contexto: Context, canvas: GraficosCanvas): Interface =
    InterfaceAndroid()
