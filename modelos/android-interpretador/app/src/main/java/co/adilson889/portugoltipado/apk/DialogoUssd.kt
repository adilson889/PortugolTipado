package co.adilson889.portugoltipado.apk

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Diálogos no estilo USSD: título, texto, campo opcional e botões Cancelar/Enviar. */
class DialogoUssd(private val atividade: Activity, private val titulo: String) {

    private var atual: Dialog? = null
    private val densidade = atividade.resources.displayMetrics.density

    private fun px(v: Int) = (v * densidade).toInt()

    /** Mostra o texto com um campo de entrada. Devolve o que foi digitado, ou null se cancelou. */
    suspend fun perguntar(texto: String): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<String?> { cont ->
            val campo = EditText(atividade)
            val d = montar(
                texto, campo, "Cancelar", "Enviar",
                aoCancelar = { if (cont.isActive) cont.resume(null) },
                aoOk = { if (cont.isActive) cont.resume(campo.text.toString()) }
            )
            cont.invokeOnCancellation { atividade.runOnUiThread { d.dismiss() } }
            atual = d
            d.show()
        }
    }

    /** Mostra o texto com um único botão OK. */
    suspend fun mostrar(texto: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { cont ->
            val d = montar(
                texto, null, null, "OK",
                aoCancelar = { if (cont.isActive) cont.resume(Unit) },
                aoOk = { if (cont.isActive) cont.resume(Unit) }
            )
            cont.invokeOnCancellation { atividade.runOnUiThread { d.dismiss() } }
            atual = d
            d.show()
        }
    }

    fun fechar() {
        try {
            atual?.dismiss()
        } catch (_: Exception) {
        }
        atual = null
    }

    private fun montar(
        texto: String,
        campo: EditText?,
        cancelar: String?,
        ok: String,
        aoCancelar: () -> Unit,
        aoOk: () -> Unit
    ): Dialog {
        val d = Dialog(atividade)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setCancelable(false)

        val raio = px(28).toFloat()
        val raiz = LinearLayout(atividade).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(24), px(28), px(24), px(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(raio, raio, raio, raio, 0f, 0f, 0f, 0f)
            }
        }

        raiz.addView(TextView(atividade).apply {
            text = titulo
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        })

        val corpo = TextView(atividade).apply {
            text = texto
            textSize = 17f
            setTextColor(0xFF222222.toInt())
            setLineSpacing(0f, 1.15f)
            setPadding(px(8), px(18), px(8), px(18))
        }
        val maxAltura = (atividade.resources.displayMetrics.heightPixels * 0.5).toInt()
        val rolagem = RolagemLimitada(atividade, maxAltura)
        rolagem.addView(corpo)
        raiz.addView(
            rolagem,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val botoes = LinearLayout(atividade).apply { orientation = LinearLayout.HORIZONTAL }
        val btOk = botao(ok, true) { d.dismiss(); aoOk() }

        if (campo != null) {
            campo.apply {
                inputType = InputType.TYPE_CLASS_TEXT
                setSingleLine(true)
                textSize = 18f
                imeOptions = EditorInfo.IME_ACTION_SEND
                setPadding(px(22), 0, px(22), 0)
                background = GradientDrawable().apply {
                    setColor(0xFFF4F4F4.toInt())
                    setStroke(px(2), 0xFF0A84FF.toInt())
                    cornerRadius = px(28).toFloat()
                }
                setOnEditorActionListener { _, acao, _ ->
                    if (acao == EditorInfo.IME_ACTION_SEND) {
                        btOk.performClick(); true
                    } else false
                }
            }
            raiz.addView(
                campo,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(56)).apply {
                    topMargin = px(8)
                    bottomMargin = px(24)
                }
            )
        }

        if (cancelar != null) {
            val btCancelar = botao(cancelar, false) { d.dismiss(); aoCancelar() }
            botoes.addView(btCancelar, LinearLayout.LayoutParams(0, px(52), 1f).apply { rightMargin = px(8) })
        }
        botoes.addView(btOk, LinearLayout.LayoutParams(0, px(52), 1f).apply { if (cancelar != null) leftMargin = px(8) })
        raiz.addView(botoes)

        d.setContentView(raiz)
        d.setOnKeyListener { _, tecla, evento ->
            if (tecla == KeyEvent.KEYCODE_BACK && evento.action == KeyEvent.ACTION_UP) {
                d.dismiss(); aoCancelar(); true
            } else false
        }
        d.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.BOTTOM)
            val teclado = if (campo != null) WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            else WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            w.setSoftInputMode(teclado or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        campo?.requestFocus()
        return d
    }

    private fun botao(texto: String, primario: Boolean, aoClicar: () -> Unit): Button =
        Button(atividade).apply {
            text = texto
            isAllCaps = false
            textSize = 18f
            setTextColor(if (primario) Color.WHITE else 0xFF444444.toInt())
            background = GradientDrawable().apply {
                setColor(if (primario) 0xFF0A84FF.toInt() else 0xFFEFEFEF.toInt())
                cornerRadius = px(26).toFloat()
            }
            setOnClickListener { aoClicar() }
        }

    /** ScrollView que não passa de uma altura máxima, para textos longos. */
    private class RolagemLimitada(contexto: Context, private val maximo: Int) : ScrollView(contexto) {
        override fun onMeasure(larguraSpec: Int, alturaSpec: Int) {
            super.onMeasure(larguraSpec, View.MeasureSpec.makeMeasureSpec(maximo, View.MeasureSpec.AT_MOST))
        }
    }
}
