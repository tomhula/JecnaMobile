package me.tomasan7.jecnamobile.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.component.OutlinedPasswordField

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
)
{
    val uiState = viewModel.uiState

    LaunchedEffect(uiState.loginResult) {
        if (uiState.loginResult is LoginResult.Success)
            onLoginSuccess()
    }

    if (uiState.isLoading)
        LoadingState()
    else
        LoginState(viewModel)
}

@Composable
private fun LoadingState()
{
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoginState(viewModel: LoginViewModel)
{
    val uiState = viewModel.uiState

    val snackbarHostState = remember { SnackbarHostState() }
    val loginResult = uiState.loginResult
    val loginErrorMessage = if (loginResult is LoginResult.Error) getLoginErrorMessage(loginResult) else null

    LaunchedEffect(loginResult) {
        if (loginResult is LoginResult.Error && loginErrorMessage != null)
            snackbarHostState.showSnackbar(loginErrorMessage)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Logo(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .size(100.dp)
            )

            Title(modifier = Modifier.padding(bottom = 50.dp))

            Column(
                modifier = Modifier.width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UsernameField(
                    onValueChanged = viewModel::onUsernameFieldValueChange,
                    value = viewModel.uiState.username,
                    isError = viewModel.uiState.usernameBlankError
                )

                Spacer(Modifier.height(10.dp))

                UserPasswordField(
                    onValueChanged = viewModel::onPasswordFieldValueChange,
                    value = uiState.password,
                    isError = uiState.passwordBlankError,
                    onDone = viewModel::onLoginClick
                )

                RememberUserCheckBox(
                    modifier = Modifier.fillMaxWidth(),
                    checked = uiState.rememberAuth,
                    onCheckedChange = viewModel::onRememberAuthCheckedChange
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canHitLogin,
                    content = { Text(stringResource(R.string.login)) },
                    onClick = { viewModel.onLoginClick() }
                )
            }
        }
    }
}

@Composable
private fun getLoginErrorMessage(loginError: LoginResult.Error) = when (loginError)
{
    is LoginResult.Error.NoInternetConnection -> stringResource(R.string.login_error_no_internet)
    is LoginResult.Error.InvalidCredentials   -> stringResource(R.string.login_error_credentials)
    is LoginResult.Error.Unknown              -> stringResource(R.string.login_error_unknown)
}

@Composable
private fun Logo(modifier: Modifier = Modifier)
{
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_jecna_foreground),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
private fun Title(modifier: Modifier = Modifier)
{
    Text(
        text = stringResource(R.string.login_title),
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineLarge,
        modifier = modifier
    )
}

@Composable
private fun RememberUserCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
)
{
    Row(
        modifier = modifier.clickable(onClick = { onCheckedChange(!checked) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) }
        )

        Text(
            text = stringResource(R.string.remember_auth_checkbox),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UsernameField(
    onValueChanged: (String) -> Unit,
    value: String, isError: Boolean,
    modifier: Modifier = Modifier
)
{
    val focusManager = LocalFocusManager.current

    val supportingText: (@Composable () -> Unit)? = if (isError)
    {
        { Text(stringResource(R.string.username_error_blank)) }
    }
    else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        isError = isError,
        label = { Text(stringResource(R.string.username)) },
        supportingText = supportingText,
        leadingIcon = { Icon(Icons.Default.Person, null) },
        singleLine = true,
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        modifier = modifier
    )
}

@Composable
private fun UserPasswordField(
    onValueChanged: (String) -> Unit,
    value: String,
    onDone: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
)
{
    val supportingText: (@Composable () -> Unit)? = if (isError)
    {
        { Text(stringResource(R.string.username_error_blank)) }
    }
    else null

    OutlinedPasswordField(
        value = value,
        onValueChange = onValueChanged,
        isError = isError,
        label = { Text(stringResource(R.string.password)) },
        supportingText = supportingText,
        singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        imeAction = ImeAction.Done,
        modifier = modifier
    )
}
