package me.tomasan7.jecnamobile.student

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import de.palm.composestateevents.EventEffect
import io.github.tomhula.jecnaapi.data.student.Guardian
import io.github.tomhula.jecnaapi.data.student.Locker
import io.github.tomhula.jecnaapi.data.student.Student
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.HorizontalSpacer
import me.tomasan7.jecnamobile.util.manipulate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun StudentProfileScreen(
    viewModel: StudentProfileViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        onDispose {
            viewModel.leftComposition()
        }
    }

    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }


    Scaffold(
        topBar = { TopAppBar(stringResource(R.string.profile_title), navigator::popBackStack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.loading,
            onRefresh = { viewModel.reload() },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                uiState.student?.let { student ->
                    StudentPicture(
                        picturePath = student.profilePicturePath,
                        imageRequestCreator = viewModel::createImageRequest
                    )

                    StudentInfoTable(student, uiState.locker, uiState.lockerLoading, uiState.lockerError)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    title: String,
    onBackClick: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}

@Composable
private fun StudentPicture(
    picturePath: String?,
    modifier: Modifier = Modifier,
    imageRequestCreator: (String) -> ImageRequest
) {
    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        if (picturePath != null)
            AsyncImage(
                modifier = Modifier
                    .padding(12.dp)
                    .aspectRatio(200f / 257f)
                    .clip(RoundedCornerShape(4.dp)),
                model = imageRequestCreator(picturePath),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
    }
}

@Composable
private fun StudentInfoTable(student: Student, locker: Locker?, lockerLoading: Boolean = false, lockerError: String? = null) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        InfoRow(stringResource(R.string.profile_full_name), student.fullName)

        student.className?.let {
            InfoRow(R.string.profile_class, it)
        }

        student.classGroups?.let {
            InfoRow(R.string.profile_class_groups, it)
        }

        student.classRegistryId?.let {
            InfoRow(R.string.profile_class_registry_id, it.toString())
        }

        student.birthDate?.let {
            InfoRow(R.string.profile_birth_date, it.format(DATE_FORMATTER))
        }

        student.birthPlace?.let {
            InfoRow(R.string.profile_birth_place, it)
        }

        student.permanentAddress?.let {
            InfoRow(R.string.profile_permanent_address, it)
        }

        InfoRow(R.string.profile_school_mail, student.schoolMail)

        student.privateMail?.let {
            InfoRow(R.string.profile_private_email, it)
        }

        student.age?.let {
            InfoRow(R.string.profile_age, "$it let")
        }

        if (student.guardians.isNotEmpty()) {
            GuardiansRow(student.guardians)
        }

        student.sposaVariableSymbol?.let {
            InfoRow(R.string.profile_sposa_vs, it)
        }

        student.sposaBankAccount?.let {
            InfoRow(R.string.profile_sposa_account, it)
        }
        

        when {
            lockerLoading -> {
                InfoRow(stringResource(R.string.locker_title), stringResource(R.string.locker_loading))
            }
            lockerError != null && lockerError.isNotBlank() -> {
                InfoRow(stringResource(R.string.locker_title), lockerError)
            }
            locker != null -> {
                LockerRow(locker)
            }
        }
    }
}

@Composable
private fun GuardiansRow(guardians: List<Guardian>) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Surface(
            tonalElevation = 20.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxHeight()
                .width(150.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.profile_guardians),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        HorizontalSpacer(size = 5.dp)

        Surface(
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                guardians.forEach { guardian ->
                    GuardianSubRow(guardian)
                }
            }
        }
    }
}

@Composable
private fun GuardianSubRow(guardian: Guardian) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = guardian.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        val contactParts = buildList {
            guardian.phoneNumber?.takeIf { it.isNotBlank() }?.let { add(it) }
            guardian.email?.takeIf { it.isNotBlank() }?.let { add(it) }
        }

        if (contactParts.isNotEmpty()) {
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = contactParts.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun LockerRow(
    locker: Locker
) {
    val separatorColor = MaterialTheme.colorScheme.surface.manipulate(0f)
    fun Modifier.separator() = this.drawWithContent {
        drawContent()
        drawLine(color = separatorColor, Offset(0f, size.height), Offset(size.width, size.height))
    }
    Column {
        LabelValueRow(
            label = stringResource(R.string.locker_title),
            valueContent = {
                SelectionContainer {
                    Text(
                        text = locker.number,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            },
            labelShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
            valueShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
            labelDrawWithContent = { separator() },
            valueDrawWithContent = { separator() }
        )
        LabelValueRow(
            label = stringResource(R.string.locker_description),
            valueContent = {
                SelectionContainer {
                    Text(
                        text = locker.description,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            },
            labelDrawWithContent = { separator() },
            valueDrawWithContent = { separator() }
        )
        LabelValueRow(
            label = stringResource(R.string.locker_assigned_from),
            valueContent = {
                SelectionContainer {
                    Text(
                        text = locker.assignedFrom?.format(DATE_FORMATTER) ?: stringResource(R.string.present),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            },
            labelDrawWithContent = { separator() },
            valueDrawWithContent = { separator() }
        )
        LabelValueRow(
            label = stringResource(R.string.locker_assigned_until),
            valueContent = {
                SelectionContainer {
                    Text(
                        text = locker.assignedUntil?.format(DATE_FORMATTER) ?: stringResource(R.string.present),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            },
            labelShape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
            valueShape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
        )
    }
}

@Composable
private fun LabelValueRow(
    label: String,
    valueContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    labelShape: RoundedCornerShape = RoundedCornerShape(4.dp),
    valueShape: RoundedCornerShape = RoundedCornerShape(4.dp),
    valueModifier: Modifier = Modifier,
    labelTonalElevation: Int = 20,
    valueTonalElevation: Int = 4,
    labelDrawWithContent: (Modifier.() -> Modifier)? = null,
    valueDrawWithContent: (Modifier.() -> Modifier)? = null
)
{
    Row(Modifier.height(IntrinsicSize.Min)) {
        var labelMod = modifier.fillMaxHeight().width(150.dp)
        if (labelDrawWithContent != null) labelMod = labelMod.labelDrawWithContent()
        Surface(
            tonalElevation = labelTonalElevation.dp,
            shape = labelShape,
            modifier = labelMod
        )
        {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            )
            {
                Text(
                    text = label,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        HorizontalSpacer(size = 5.dp)
        var valueMod = valueModifier.fillMaxSize()
        if (valueDrawWithContent != null) valueMod = valueMod.valueDrawWithContent()
        Surface(
            tonalElevation = valueTonalElevation.dp,
            shape = valueShape,
            modifier = valueMod
        )
        {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                valueContent()
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
)
{
    LabelValueRow(
        label = label,
        valueContent = {
            SelectionContainer {
                Text(
                    text = value,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun InfoRow(
    @StringRes label: Int,
    value: String,
    modifier: Modifier = Modifier
)
{
    InfoRow(label = stringResource(label), value = value, modifier = modifier)
}


private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.y")
