
package com.dompetbulanan.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences("dompet", MODE_PRIVATE)
    }
    private val items = JSONArray()
    private lateinit var root: LinearLayout
    private lateinit var summary: TextView
    private lateinit var list: LinearLayout
    private val money =
        NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val old = JSONArray(
            prefs.getString("transactions", "[]") ?: "[]"
        )
        for (i in 0 until old.length()) {
            items.put(old.getJSONObject(i))
        }
        render()
    }

    private fun save() {
        prefs.edit().putString("transactions", items.toString()).apply()
    }

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun text(
        value: String,
        size: Float = 16f,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(0xFF17352B.toInt())
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(dp(4), dp(6), dp(4), dp(6))
    }

    private fun button(
        label: String,
        action: () -> Unit
    ): Button = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(16))
            setBackgroundColor(0xFFF4F8F5.toInt())
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(text("Dompet Bulanan", 27f, true))
        content.addView(text(
            "Catat uang masuk dan keluar dengan mudah.", 14f
        ))

        summary = text("", 19f, true)
        summary.setPadding(dp(14), dp(18), dp(14), dp(18))
        summary.setBackgroundColor(0xFFDDEFE4.toInt())
        content.addView(summary)

        content.addView(button("+ Tambah transaksi") {
            addTransaction()
        })

        content.addView(button("Laporan bulan ini") {
            showReport()
        })

        content.addView(text("Riwayat transaksi", 20f, true))

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(list)
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        updateData()
    }

    private fun updateData() {
        var income = 0L
        var expense = 0L
        list.removeAllViews()

        for (i in items.length() - 1 downTo 0) {
            val obj = items.getJSONObject(i)
            val amount = obj.optLong("amount")

            if (obj.optString("type") == "Pemasukan") {
                income += amount
            } else {
                expense += amount
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setBackgroundColor(0xFFFFFFFF.toInt())
            }

            val sign =
                if (obj.optString("type") == "Pemasukan") "+" else "-"

            row.addView(text(
                "${obj.optString("title")} • ${obj.optString("category")}",
                16f, true
            ))

            row.addView(text(
                "$sign${money.format(amount)} | ${obj.optString("date")}",
                14f
            ))

            row.addView(button("Hapus transaksi") {
                AlertDialog.Builder(this)
                    .setMessage("Hapus transaksi ini?")
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Hapus") { _, _ ->
                        val updated = JSONArray()
                        for (j in 0 until items.length()) {
                            if (j != i) {
                                updated.put(items.getJSONObject(j))
                            }
                        }
                        while (items.length() > 0) {
                            items.remove(items.length() - 1)
                        }
                        for (j in 0 until updated.length()) {
                            items.put(updated.getJSONObject(j))
                        }
                        save()
                        updateData()
                    }.show()
            })

            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(8)
            list.addView(row, params)
        }

        summary.text =
            "Saldo: ${money.format(income - expense)}\n" +
            "Pemasukan: ${money.format(income)}\n" +
            "Pengeluaran: ${money.format(expense)}"

        if (items.length() == 0) {
            list.addView(text(
                "Belum ada transaksi. Tekan tombol tambah untuk mulai."
            ))
        }
    }

    private fun addTransaction() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }

        val title = EditText(this).apply {
            hint = "Nama transaksi"
        }

        val amount = EditText(this).apply {
            hint = "Jumlah (contoh: 50000)"
            inputType = 2
        }

        val type = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Pengeluaran", "Pemasukan")
            )
        }

        val category = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "Makanan", "Transportasi", "Belanja",
                    "Tagihan", "Gaji", "Lainnya"
                )
            )
        }

        panel.addView(text("Nama transaksi"))
        panel.addView(title)
        panel.addView(text("Jumlah rupiah"))
        panel.addView(amount)
        panel.addView(text("Jenis transaksi"))
        panel.addView(type)
        panel.addView(text("Kategori"))
        panel.addView(category)

        AlertDialog.Builder(this)
            .setTitle("Transaksi baru")
            .setView(panel)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val value = amount.text.toString().toLongOrNull()

                if (title.text.isNullOrBlank() ||
                    value == null || value <= 0
                ) {
                    Toast.makeText(
                        this,
                        "Isi nama dan jumlah yang benar",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val obj = JSONObject()
                        .put("title", title.text.toString())
                        .put("amount", value)
                        .put("type", type.selectedItem.toString())
                        .put("category", category.selectedItem.toString())
                        .put(
                            "date",
                            java.text.SimpleDateFormat(
                                "dd-MM-yyyy",
                                Locale("in", "ID")
                            ).format(java.util.Date())
                        )

                    items.put(obj)
                    save()
                    updateData()
                }
            }.show()
    }

    private fun showReport() {
        var income = 0L
        var expense = 0L

        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            if (o.optString("type") == "Pemasukan") {
                income += o.optLong("amount")
            } else {
                expense += o.optLong("amount")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Ringkasan keuangan")
            .setMessage(
                "Total pemasukan: ${money.format(income)}\n" +
                "Total pengeluaran: ${money.format(expense)}\n" +
                "Sisa saldo: ${money.format(income - expense)}"
            )
            .setPositiveButton("Tutup", null)
            .show()
    }
}
