package co.adilson889.portugoltipado.apk

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Activity sem ecrã próprio: ao abrir, corre o programa .port e cada
 * escreva/leia aparece num diálogo nativo do Android, como uma sessão USSD.
 */
class PortugolActivity : Activity() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tarefa: Job? = null
    private var ui: DialogoUssd? = null

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        val nome = applicationInfo.loadLabel(packageManager).toString()
        val dialogo = DialogoUssd(this, nome)
        ui = dialogo
        tarefa = escopo.launch {
            try {
                Executor(this@PortugolActivity, dialogo).correr()
            } finally {
                finish()
            }
        }
    }

    override fun onDestroy() {
        tarefa?.cancel()
        ui?.fechar()
        super.onDestroy()
    }
}
