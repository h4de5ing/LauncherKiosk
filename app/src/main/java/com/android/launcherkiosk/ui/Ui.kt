package com.android.launcherkiosk.ui

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun screenRoot(context: Context): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(context.dp(20))
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}

fun titleView(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    textSize = 26f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(0xFF111827.toInt())
}

fun bodyView(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    textSize = 15f
    setTextColor(0xFF4B5563.toInt())
    setPadding(0, context.dp(8), 0, context.dp(16))
}

fun actionButton(context: Context, text: String, onClick: () -> Unit): Button = Button(context).apply {
    this.text = text
    setOnClickListener { onClick() }
}
