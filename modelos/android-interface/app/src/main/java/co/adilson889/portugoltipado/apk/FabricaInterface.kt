package co.adilson889.portugoltipado.apk

import android.content.Context
import co.adilson889.typec.graficos.GraficosCanvas
import co.adilson889.typec.interfaceui.Interface
import co.adilson889.typec.interfaceui.InterfaceAndroid

/**
 * Único sítio do modelo que conhece o construtor da InterfaceAndroid.
 * Se o teu for diferente (por exemplo, se também pedir o contexto), ajusta só esta linha.
 */
internal fun criarInterface(contexto: Context, canvas: GraficosCanvas): Interface =
    InterfaceAndroid(canvas)
