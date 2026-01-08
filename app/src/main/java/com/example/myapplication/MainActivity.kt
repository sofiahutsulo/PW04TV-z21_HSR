package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btnFillControl = findViewById<Button>(R.id.btnFillControl)
        val btnCalc = findViewById<Button>(R.id.btnCalcPr4)
        val tv = findViewById<TextView>(R.id.tvResult)


        val edP = findViewById<EditText>(R.id.edP_kw)
        val edU = findViewById<EditText>(R.id.edU_kv)
        val edCos = findViewById<EditText>(R.id.edCos)
        val edJ = findViewById<EditText>(R.id.edJ)


        val edUnGpp = findViewById<EditText>(R.id.edUn_gpp)
        val edSk3Gpp = findViewById<EditText>(R.id.edSk3_gpp)
        val edK1Gpp = findViewById<EditText>(R.id.edK1_gpp)


        val n = ModeViews(
            ipC = findViewById(R.id.edIpc_n),
            ipD = findViewById(R.id.edIpd_n),
            taC = findViewById(R.id.edTac_n),
            taD = findViewById(R.id.edTad_n),
            gamma = findViewById(R.id.edGamma_n),
            t = findViewById(R.id.edT_n),
            tVyk = findViewById(R.id.edTvyk_n),
            name = "Нормальний"
        )
        val min = ModeViews(
            ipC = findViewById(R.id.edIpc_min),
            ipD = findViewById(R.id.edIpd_min),
            taC = findViewById(R.id.edTac_min),
            taD = findViewById(R.id.edTad_min),
            gamma = findViewById(R.id.edGamma_min),
            t = findViewById(R.id.edT_min),
            tVyk = findViewById(R.id.edTvyk_min),
            name = "Мінімальний"
        )
        val av = ModeViews(
            ipC = findViewById(R.id.edIpc_av),
            ipD = findViewById(R.id.edIpd_av),
            taC = findViewById(R.id.edTac_av),
            taD = findViewById(R.id.edTad_av),
            gamma = findViewById(R.id.edGamma_av),
            t = findViewById(R.id.edT_av),
            tVyk = findViewById(R.id.edTvyk_av),
            name = "Аварійний"
        )

        btnFillControl.setOnClickListener {
            n.ipC.setText("3.9")
            n.ipD.setText("0.7")
            n.taC.setText("0.03")
            n.taD.setText("0.037")
            n.gamma.setText("0.71")
            n.t.setText("0.065")
            n.tVyk.setText("0.6")
        }

        btnCalc.setOnClickListener {
            val pKw = readDouble(edP)
            val uKv = readDouble(edU)
            val cos = readDouble(edCos)
            val j = readDouble(edJ)

            val unGpp = readDouble(edUnGpp)
            val sk3Gpp = readDouble(edSk3Gpp)
            val k1Gpp = readDouble(edK1Gpp)

            if (pKw == null || uKv == null || cos == null || j == null ||
                unGpp == null || sk3Gpp == null || k1Gpp == null
            ) {
                tv.text = "Перевірте введені дані: у частинах 1 і 2 всі поля мають бути заповнені числами."
                return@setOnClickListener
            }

            val iLoadA = (pKw * 1000.0) / (sqrt(3.0) * (uKv * 1000.0) * cos)

            val sNeedMm2 = iLoadA / j

            val sStd = listOf(35, 50, 70, 95, 120, 150, 185, 240)
            val sPick = sStd.firstOrNull { it.toDouble() >= sNeedMm2 } ?: sStd.last()

            val ik3_gpp = sk3Gpp / (sqrt(3.0) * unGpp)
            val ik1_gpp = k1Gpp * ik3_gpp

            val rN = calcMode(n)
            val rMin = calcMode(min)
            val rAv = calcMode(av)

            if (rN == null || rMin == null || rAv == null) {
                tv.text = "Перевірте введені дані у частині 3 (7.4): для кожного режиму потрібні всі поля."
                return@setOnClickListener
            }

            tv.text = buildString {
                appendLine("ПРАКТИЧНА РОБОТА 4 — результат")
                appendLine()

                appendLine("1) Вибір кабелю (7.1) — розрахунок по струму навантаження")
                appendLine("P = ${fmt(pKw)} кВт; U = ${fmt(uKv)} кВ; cosφ = ${fmt(cos)}; Jдоп = ${fmt(j)} А/мм²")
                appendLine("Iнавантаж = ${fmt(iLoadA)} А")
                appendLine("Sпотр = I/J = ${fmt(sNeedMm2)} мм²")
                appendLine("Рекомендований стандартний переріз: ${sPick} мм²")
                appendLine()

                appendLine("2) Струми КЗ на шинах 10 кВ ГПП (7.2)")
                appendLine("Un = ${fmt(unGpp)} кВ; Sk3 = ${fmt(sk3Gpp)} МВА; k1ф = ${fmt(k1Gpp)}")
                appendLine("Iк3 = ${fmt(ik3_gpp)} кА")
                appendLine("Iк1 = ${fmt(ik1_gpp)} кА")
                appendLine()

                appendLine("3) ХПнЕМ (7.4) — 3 режими, аперіодична складова, ударний струм, тепловий імпульс")
                appendLine(rN.pretty())
                appendLine(rMin.pretty())
                appendLine(rAv.pretty())
            }
        }
    }


    private fun calcMode(v: ModeViews): ModeResult? {
        val ipC = readDouble(v.ipC) ?: return null
        val ipD = readDouble(v.ipD) ?: return null
        val taC = readDouble(v.taC) ?: return null
        val taD = readDouble(v.taD) ?: return null
        val gamma = readDouble(v.gamma) ?: return null
        val t = readDouble(v.t) ?: return null
        val tVyk = readDouble(v.tVyk) ?: return null

        if (gamma <= 0.0 || gamma >= 1.0) return null
        if (taC <= 0.0 || taD <= 0.0) return null

        val sqrt2 = sqrt(2.0)

        val iaC = sqrt2 * ipC * kotlin.math.exp(-t / taC)
        val iaD = sqrt2 * ipD * kotlin.math.exp(-t / taD)

        val iudC = sqrt2 * ipC * (1.0 + kotlin.math.exp(-0.01 / taC))
        val iudD = sqrt2 * ipD * (1.0 + kotlin.math.exp(-0.01 / taD))

        val taEq = (taC * ipC + taD * ipD) / (ipC + ipD)

        val tpD = -t / ln(gamma)

        val bk = ipC * ipC * (tVyk + taEq) +
                ipD * ipD * (0.5 * tpD + taEq) +
                2.0 * ipC * ipD * (tpD + taEq)

        return ModeResult(
            name = v.name,
            ipC = ipC, ipD = ipD,
            taC = taC, taD = taD,
            gamma = gamma, t = t, tVyk = tVyk,
            iaC = iaC, iaD = iaD,
            iudC = iudC, iudD = iudD,
            taEq = taEq, tpD = tpD, bk = bk
        )
    }

    private fun readDouble(et: EditText): Double? {
        val s = et.text.toString().trim().replace(",", ".")
        return s.toDoubleOrNull()
    }

    private fun fmt(x: Double, d: Int = 2): String =
        String.format(Locale.US, "%.${d}f", x)

    private data class ModeViews(
        val ipC: EditText,
        val ipD: EditText,
        val taC: EditText,
        val taD: EditText,
        val gamma: EditText,
        val t: EditText,
        val tVyk: EditText,
        val name: String
    )

    private data class ModeResult(
        val name: String,
        val ipC: Double, val ipD: Double,
        val taC: Double, val taD: Double,
        val gamma: Double, val t: Double, val tVyk: Double,
        val iaC: Double, val iaD: Double,
        val iudC: Double, val iudD: Double,
        val taEq: Double, val tpD: Double, val bk: Double
    ) {
        fun pretty(): String = buildString {
            appendLine("— $name режим —")
            appendLine("Вхідні: Iп0_с=${ipC.f()} кА; Iп0_д=${ipD.f()} кА; Ta_с=${taC.f(3)} c; Ta_д=${taD.f(3)} c; γ=${gamma.f(2)}; t=${t.f(3)} c; tвик=${tVyk.f(2)} c")
            appendLine("Аперіодична складова при t:")
            appendLine("i_a,с = √2·Iп0_с·e^(-t/Ta_с) = ${iaC.f()} кА")
            appendLine("i_a,д = √2·Iп0_д·e^(-t/Ta_д) = ${iaD.f()} кА")
            appendLine("Ударний струм:")
            appendLine("i_уд,с = √2·Iп0_с·(1+e^(-0.01/Ta_с)) = ${iudC.f()} кА")
            appendLine("i_уд,д = √2·Iп0_д·(1+e^(-0.01/Ta_д)) = ${iudD.f()} кА")
            appendLine("Ta,екв = ${taEq.f(3)} c; Tп.д = ${tpD.f(2)} c")
            appendLine("Тепловий імпульс Bk = ${bk.f(2)} кА²·с")
            appendLine()
        }

        private fun Double.f(d: Int = 2): String =
            String.format(Locale.US, "%.${d}f", this)
    }
}
