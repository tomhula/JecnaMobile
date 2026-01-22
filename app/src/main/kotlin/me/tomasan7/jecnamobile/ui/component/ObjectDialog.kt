package me.tomasan7.jecnamobile.ui.component

import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun <T> ObjectDialog(
    state: ObjectDialogState<T>,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable (T) -> Unit
)
{
    if (state.shown)
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties
        )
        {
            content(state.value!!)
        }
}

@Stable
class ObjectDialogState<T>
{
    var shown by mutableStateOf(false)
    var value by mutableStateOf<T?>(null)

    fun show(value: T)
    {
        this.value = value
        shown = true
    }

    fun hide()
    {
        shown = false
    }
}

@Composable
fun <T> rememberObjectDialogState() = remember { ObjectDialogState<T>() }
