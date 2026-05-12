package com.voxpocket.util

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

@Composable
fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    val context = LocalContext.current
    Toast.makeText(context, message, duration).show()
}
