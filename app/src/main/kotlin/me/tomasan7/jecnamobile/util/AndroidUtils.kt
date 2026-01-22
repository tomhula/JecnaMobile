package me.tomasan7.jecnamobile.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

fun createBroadcastReceiver(handler: BroadcastReceiver.(Context, Intent) -> Unit) = object : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent) = handler(context, intent)
}
