package me.tomasan7.jecnamobile.student

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun StudentProfileScreen(
    viewModel: StudentProfileViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
)
{
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
)
{
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
)
{
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
private fun StudentInfoTable(
    student: Student,
    locker: Locker?,
    lockerLoading: Boolean = false,
    lockerError: String? = null
)
{
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TextTableField(R.string.profile_full_name, student.fullName)

        TextTableField(R.string.profile_class, student.className)
        TextTableField(R.string.profile_class_groups, student.classGroups)
        TextTableField(R.string.profile_class_registry_id, student.classRegistryId?.toString())
        TextTableField(R.string.profile_birth_date, student.birthDate?.format(DATE_FORMATTER))
        TextTableField(R.string.profile_birth_place, student.birthPlace)
        TextTableField(R.string.profile_permanent_address, student.permanentAddress)
        TextTableField(R.string.profile_school_mail, student.schoolMail)
        TextTableField(R.string.profile_private_email, student.privateMail)
        TextTableField(R.string.profile_age, student.age?.let { stringResource(R.string.profile_age_value, it) })
        GuardiansTableField(student.guardians)
        TextTableField(R.string.profile_sposa_vs, student.sposaVariableSymbol)
        TextTableField(R.string.profile_sposa_account, student.sposaBankAccount)

        when
        {
            lockerLoading -> TextTableField(R.string.profile_locker_title, stringResource(R.string.profile_locker_loading))
            lockerError != null -> TextTableField(R.string.profile_locker_title, lockerError)
            locker != null -> LockerFieldGroup(locker)
        }
    }
}

@Composable
private fun GuardiansTableField(guardians: List<Guardian>)
{
    TableField(
        label = stringResource(R.string.profile_guardians),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(16.dp)
        ) {
            guardians.forEach { guardian ->
                GuardiansValue(guardian)
            }
        }
    }
}

@Composable
private fun GuardiansValue(guardian: Guardian)
{
    Column {
        Text(
            text = guardian.name,
            style = MaterialTheme.typography.bodyMedium,
        )

        val contact = buildString {
            guardian.phoneNumber?.let { append(it) }
            if (guardian.phoneNumber != null && guardian.email != null)
                append(" • ")
            guardian.email?.let { append(it) }
        }

        if (contact.isNotEmpty())
            Text(
                text = contact,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun LockerFieldGroup(locker: Locker)
{
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        TextTableField(
            label = R.string.profile_locker_title,
            value = locker.number,
            roundedTop = true,
        )
        TextTableField(
            label = R.string.profile_locker_description,
            value = locker.description,
        )
        TextTableField(
            label = R.string.profile_locker_assigned_from,
            value = locker.assignedFrom?.format(DATE_FORMATTER) ?: stringResource(R.string.profile_locker_present),
        )
        TextTableField(
            label = R.string.profile_locker_assigned_until,
            value = locker.assignedUntil?.format(DATE_FORMATTER) ?: stringResource(R.string.profile_locker_present),
            roundedBottom = true
        )
    }
}

@Composable
private fun TextTableField(
    @StringRes
    label: Int,
    value: String?,
    modifier: Modifier = Modifier,
    roundedTop: Boolean = false,
    roundedBottom: Boolean = false,
) = TableField(stringResource(label), modifier, roundedTop, roundedBottom) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {

            SelectionContainer {
                Text(
                    text = value ?: "",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

@Composable
private fun TableField(
    label: String,
    modifier: Modifier = Modifier,
    roundedTop: Boolean = false,
    roundedBottom: Boolean = false,
    value: @Composable () -> Unit,
)
{
    var shapeTopCornerRadius = 0.dp
    var shapeBottomCornerRadius = 0.dp

    if (roundedBottom)
        shapeBottomCornerRadius = 4.dp
    else if (roundedTop)
        shapeTopCornerRadius = 4.dp

    val shape = RoundedCornerShape(topStart = shapeTopCornerRadius, topEnd = shapeTopCornerRadius, bottomStart = shapeBottomCornerRadius, bottomEnd = shapeBottomCornerRadius)

    Row(
        modifier = Modifier.height(IntrinsicSize.Min)
    ) {
        Surface(
            tonalElevation = 20.dp,
            shape = shape,
            modifier = modifier.fillMaxHeight().width(150.dp)
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            )
            {
                Text(
                    text = label,
                    modifier = Modifier
                )
            }
        }

        HorizontalSpacer(size = 5.dp)

        Surface(
            tonalElevation = 4.dp,
            shape = shape,
            modifier = Modifier.fillMaxSize(),
            content = value,
        )
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy")
