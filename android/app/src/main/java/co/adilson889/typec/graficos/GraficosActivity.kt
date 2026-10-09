package co.adilson889.typec.interfaceui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import co.adilson889.typec.graficos.GraficosView
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt

/**
 * Implementacao Android da biblioteca 'interface': uma WebView por componente,
 * posta numa camada ([raiz]) por cima da GraficosView.
 *
 * Criada antes da janela existir (o GraficosCanvas guarda-a); a GraficosActivity
 * liga-a a camada e a View com [ligar]. Ate la, os componentes ficam so no modelo.
 *
 * Posicao e tamanho seguem a transformacao da GraficosView (letterbox, zoom e pan).
 * O interpretador corre na thread de UI; as chamadas do JS chegam em threads de
 * fundo e so tocam em estruturas concorrentes.
 */
@SuppressLint("SetJavaScriptEnabled")
class InterfaceAndroid : Interface {

    private class Campo(val valor: String, val marcado: Boolean)

    private class Toque(val ids: Set<String>, val classes: Map<String, Int>) {
        /** -1 = acertou num id; >= 0 = indice na classe; null = nao acertou. */
        fun atinge(alvo: String): Int? = when {
            alvo.startsWith(".") -> classes[alvo.substring(1)]
            alvo in ids -> -1
            else -> null
        }
    }

    private class Slot {
        var ret = IntArray(4)
        var html: String? = null
        var webView: WebView? = null
        var pronto = false
        var htmlEnviado: String? = null
        var chaveLayout: IntArray? = null
        var escalaEnviada: String? = null
    }

    private val main = Handler(Looper.getMainLooper())

    // So na thread de UI
    private val slots = mutableListOf<Slot>()
    private var raiz: FrameLayout? = null
    private var view: GraficosView? = null
    private var transformacaoPronta = false

    // Concorrentes (escritos pela ponte JS)
    private val toques = ConcurrentLinkedQueue<Toque>()
    private val campos = ConcurrentHashMap<Int, ConcurrentHashMap<String, Campo>>()
    private val contagens = ConcurrentHashMap<Int, Map<String, Int>>()

    @Volatile private var toqueDoQuadro: Toque? = null
    @Volatile private var ultimoIndice = -1

    private fun naMain(bloco: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) bloco() else main.post { bloco() }
    }

    // ---------------- ligacao com a Activity ----------------

    fun ligar(raiz: FrameLayout, view: GraficosView) {
        naMain {
            this.raiz = raiz
            this.view = view
            transformacaoPronta = false
            view.aoMudarTransformacao = { transformacaoMudou() }
            for (i in slots.indices) atualizarWebView(i, slots[i])
        }
    }

    override fun liberar() {
        naMain {
            view?.aoMudarTransformacao = null
            for (s in slots) destruir(s)
            slots.clear()
            campos.clear()
            contagens.clear()
            toques.clear()
            raiz = null
            view = null
            transformacaoPronta = false
        }
    }

    private fun destruir(s: Slot) {
        val wv = s.webView ?: return
        raiz?.removeView(wv)
        wv.destroy()
        s.webView = null
        s.pronto = false
    }

    private fun transformacaoMudou() {
        transformacaoPronta = true
        for (i in slots.indices) atualizarWebView(i, slots[i])
    }

    // ---------------- contrato ----------------

    override fun componente(indice: Int, x: Int, y: Int, largura: Int, altura: Int, html: String) {
        naMain {
            while (slots.size <= indice) slots.add(Slot())
            val s = slots[indice]
            s.ret = intArrayOf(x, y, largura, altura)
            s.html = html
            atualizarWebView(indice, s)
        }
    }

    override fun fimQuadro(total: Int) {
        naMain {
            while (slots.size > total) {
                val i = slots.size - 1
                destruir(slots.removeAt(i))
                campos.remove(i)
                contagens.remove(i)
            }
        }
        toqueDoQuadro = toques.poll()
        ultimoIndice = -1
    }

    override fun clique(alvo: String): Boolean {
        val indice = toqueDoQuadro?.atinge(alvo) ?: return false
        ultimoIndice = indice
        return true
    }

    override fun indiceClique(): Int = ultimoIndice

    private fun procurar(id: String): Campo? {
        for (m in campos.values) m[id]?.let { return it }
        return null
    }

    override fun leiaCampo(id: String): String? = procurar(id)?.valor
    override fun valor(id: String): String = procurar(id)?.valor ?: ""
    override fun marcado(id: String): Boolean = procurar(id)?.marcado ?: false

    override fun definaValor(id: String, texto: String) {
        for (m in campos.values) m[id]?.let { m[id] = Campo(texto, it.marcado) }
        naMain {
            val js = "__definir(${JSONObject.quote(id)},${JSONObject.quote(texto)})"
            for (s in slots) if (s.pronto) s.webView?.evaluateJavascript(js, null)
        }
    }

    // ---------------- WebViews (thread de UI) ----------------

    private fun atualizarWebView(indice: Int, s: Slot) {
        val r = raiz ?: return
        val html = s.html ?: return
        val wv = s.webView ?: criarWebView(r, indice, s)
        if (!transformacaoPronta) return

        posicionar(s, wv)
        if (s.pronto) {
            if (html != s.htmlEnviado) enviar(s, html) else enviarEscala(s)
        }
    }

    private fun criarWebView(r: FrameLayout, indice: Int, s: Slot): WebView {
        val wv = WebView(r.context)
        s.webView = wv
        wv.visibility = View.INVISIBLE // so aparece quando a posicao certa for conhecida
        wv.setBackgroundColor(Color.TRANSPARENT)
        wv.overScrollMode = View.OVER_SCROLL_NEVER
        wv.isVerticalScrollBarEnabled = false
        wv.isHapticFeedbackEnabled = false // sem vibracao no toque longo: so os campos tem comportamento de texto
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            blockNetworkLoads = false
            allowFileAccess = true            // imagens e fontes do projeto (assets)
            allowContentAccess = true
            setSupportZoom(false)             // sem zoom com os dedos
            builtInZoomControls = false
            displayZoomControls = false
        }
        wv.addJavascriptInterface(Ponte(indice), "Ponte")
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView?, url: String?) {
                s.pronto = true
                s.html?.let { enviar(s, it) }
            }

            // Links externos abrem no navegador; a janela do programa nao navega
            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                val url = req?.url ?: return true
                val esq = url.scheme
                if (esq == "http" || esq == "https" || esq == "mailto" || esq == "tel") {
                    try {
                        v?.context?.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {
                    }
                }
                return true
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(v: WebView?, url: String?, msg: String?, r: JsResult): Boolean {
                val ctx = v?.context ?: return false
                AlertDialog.Builder(ctx)
                    .setMessage(msg)
                    .setPositiveButton("OK") { _, _ -> r.confirm() }
                    .setOnCancelListener { r.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(v: WebView?, url: String?, msg: String?, r: JsResult): Boolean {
                val ctx = v?.context ?: return false
                AlertDialog.Builder(ctx)
                    .setMessage(msg)
                    .setPositiveButton("OK") { _, _ -> r.confirm() }
                    .setNegativeButton("Cancelar") { _, _ -> r.cancel() }
                    .setOnCancelListener { r.cancel() }
                    .show()
                return true
            }
        }
        r.addView(wv, FrameLayout.LayoutParams(1, 1))
        // Base nos assets do projeto: <img src="logo.png"> resolve para assets/projeto/logo.png
        wv.loadDataWithBaseURL("file:///android_asset/projeto/", MODELO, "text/html", "utf-8", null)
        return wv
    }

    private fun posicionar(s: Slot, wv: WebView) {
        val g = view ?: return
        val e = g.escalaAtual()
        val esq = (g.deslocXAtual() + s.ret[0] * e).roundToInt()
        val topo = (g.deslocYAtual() + s.ret[1] * e).roundToInt()
        val larg = maxOf(1, (s.ret[2] * e).roundToInt())
        val alt = maxOf(1, (s.ret[3] * e).roundToInt())
        val chave = intArrayOf(esq, topo, larg, alt)
        if (s.chaveLayout?.contentEquals(chave) != true) {
            s.chaveLayout = chave
            val lp = FrameLayout.LayoutParams(larg, alt)
            lp.gravity = Gravity.TOP or Gravity.START
            lp.leftMargin = esq
            lp.topMargin = topo
            wv.layoutParams = lp
        }
        if (wv.visibility != View.VISIBLE) wv.visibility = View.VISIBLE
    }

    /** Largura em pixels da janela e zoom CSS (pixels do ecra por pixel CSS da WebView). */
    private fun parametrosEscala(s: Slot): Pair<Int, Float>? {
        val g = view ?: return null
        val wv = s.webView ?: return null
        val k = g.escalaAtual() / wv.resources.displayMetrics.density
        return s.ret[2] to k
    }

    private fun enviar(s: Slot, html: String) {
        val wv = s.webView ?: return
        val (l, k) = parametrosEscala(s) ?: return
        s.htmlEnviado = html
        s.escalaEnviada = "$l/$k"
        wv.evaluateJavascript("__atualizar(${JSONObject.quote(html)},$l,$k)", null)
    }

    private fun enviarEscala(s: Slot) {
        val wv = s.webView ?: return
        val (l, k) = parametrosEscala(s) ?: return
        val chave = "$l/$k"
        if (chave == s.escalaEnviada) return
        s.escalaEnviada = chave
        wv.evaluateJavascript("__escala($l,$k)", null)
    }

    // ---------------- ponte JS -> Kotlin (threads de fundo) ----------------

    private inner class Ponte(private val indice: Int) {
        @JavascriptInterface
        fun clique(json: String) {
            val o = JSONObject(json)
            val ids = HashSet<String>()
            o.getJSONArray("ids").let { a -> for (i in 0 until a.length()) ids.add(a.getString(i)) }
            val classes = HashMap<String, Int>()
            o.getJSONArray("classes").let { a ->
                for (i in 0 until a.length()) {
                    val par = a.getJSONArray(i)
                    val nome = par.getString(0)
                    // indice global: soma dos elementos da classe nos componentes anteriores
                    var antes = 0
                    for (j in 0 until indice) antes += contagens[j]?.get(nome) ?: 0
                    classes[nome] = par.getInt(1) + antes
                }
            }
            toques.add(Toque(ids, classes))
        }

        @JavascriptInterface
        fun campo(id: String, valor: String, marcado: Boolean) {
            campos.getOrPut(indice) { ConcurrentHashMap() }[id] = Campo(valor, marcado)
        }

        @JavascriptInterface
        fun estado(json: String) {
            val o = JSONObject(json)
            val c = o.getJSONObject("c")
            val mapa = HashMap<String, Int>()
            for (k in c.keys()) mapa[k] = c.getInt(k)
            contagens[indice] = mapa

            val f = o.getJSONObject("f")
            val atuais = campos.getOrPut(indice) { ConcurrentHashMap() }
            atuais.keys.retainAll(f.keys().asSequence().toSet())
            for (id in f.keys()) {
                val par = f.getJSONArray(id)
                atuais[id] = Campo(par.getString(0), par.getBoolean(1))
            }
        }
    }

    private companion object {
        // JS interno da biblioteca: o utilizador nunca o escreve nem o ve.
        // __morph aplica so as diferencas e preserva valor/foco/cursor/scroll dos campos.
        val MODELO = """
<!DOCTYPE html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>html,body{margin:0;padding:0;background:transparent}#__raiz{box-sizing:border-box}*{-webkit-user-select:none;user-select:none;-webkit-touch-callout:none;-webkit-tap-highlight-color:transparent}input,textarea,select{-webkit-user-select:text;user-select:text}</style>
</head><body><div id="__raiz"></div><script>
function __campo(e){var t=e.nodeName;return t==='INPUT'||t==='TEXTAREA'||t==='SELECT';}
document.addEventListener('contextmenu',function(ev){if(!__campo(ev.target))ev.preventDefault();});
function __enviar(e){Ponte.campo(e.id,String(e.value),!!e.checked);}
function __sync(x,y){
  var i,a;
  for(i=x.attributes.length-1;i>=0;i--){a=x.attributes[i].name;
    if(!y.hasAttribute(a)&&!(__campo(x)&&(a==='value'||a==='checked')))x.removeAttribute(a);}
  for(i=0;i<y.attributes.length;i++){a=y.attributes[i];
    if(__campo(x)&&(a.name==='value'||a.name==='checked'))continue;
    if(x.getAttribute(a.name)!==a.value)x.setAttribute(a.name,a.value);}
}
function __morph(a,b){
  var ac=a.childNodes,bc=b.childNodes,i,j,x,y,m;
  for(i=0;i<bc.length;i++){
    x=ac[i];y=bc[i];
    if(y.nodeType===1&&y.id){
      m=null;
      for(j=i;j<ac.length;j++){if(ac[j].nodeType===1&&ac[j].id===y.id){m=ac[j];break;}}
      if(m&&m!==x){a.insertBefore(m,x||null);x=m;}
    }
    if(!x){a.appendChild(y.cloneNode(true));continue;}
    if(x.nodeType!==y.nodeType||x.nodeName!==y.nodeName||(x.nodeType===1&&x.id!==y.id)){
      a.replaceChild(y.cloneNode(true),x);continue;}
    if(x.nodeType!==1){if(x.nodeValue!==y.nodeValue)x.nodeValue=y.nodeValue;continue;}
    __sync(x,y);
    if(!__campo(x))__morph(x,y);
  }
  while(ac.length>bc.length)a.removeChild(a.lastChild);
}
function __estado(){
  var c={},f={},els=document.getElementById('__raiz').getElementsByTagName('*'),i,j,e,n;
  for(i=0;i<els.length;i++){
    e=els[i];
    if(e.classList)for(j=0;j<e.classList.length;j++){n=e.classList[j];c[n]=(c[n]||0)+1;}
    if(e.id&&__campo(e))f[e.id]=[String(e.value),!!e.checked];
  }
  Ponte.estado(JSON.stringify({c:c,f:f}));
}
function __escala(l,k){var r=document.getElementById('__raiz');r.style.width=l+'px';r.style.zoom=k;}
function __atualizar(html,l,k){
  var r=document.getElementById('__raiz'),t=document.createElement('div');
  __escala(l,k);
  t.innerHTML=html;
  __morph(r,t);
  __estado();
}
function __definir(id,v){var e=document.getElementById(id);if(e){e.value=v;__enviar(e);}}
document.addEventListener('input',function(ev){var e=ev.target;if(e.id&&__campo(e))__enviar(e);},true);
document.addEventListener('change',function(ev){var e=ev.target;if(e.id&&__campo(e))__enviar(e);},true);
document.addEventListener('click',function(ev){
  var e=ev.target,ids=[],cl=[],vistos={},j,n;
  while(e&&e.nodeType===1&&e.id!=='__raiz'){
    if(e.id)ids.push(e.id);
    if(e.classList)for(j=0;j<e.classList.length;j++){
      n=e.classList[j];
      if(!vistos[n]){vistos[n]=1;cl.push([n,Array.prototype.indexOf.call(document.getElementsByClassName(n),e)]);}
    }
    e=e.parentNode;
  }
  Ponte.clique(JSON.stringify({ids:ids,classes:cl}));
},true);
</script></body></html>
"""
    }
}