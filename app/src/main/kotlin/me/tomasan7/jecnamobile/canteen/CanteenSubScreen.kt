package me.tomasan7.jecnamobile.canteen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import io.github.tomhula.jecnaapi.data.canteen.DayMenu
import io.github.tomhula.jecnaapi.data.canteen.ExchangeItem
import io.github.tomhula.jecnaapi.data.canteen.MenuItem
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.canteen.ExchangeDay
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.ui.component.Card
import me.tomasan7.jecnamobile.ui.component.DialogContainer
import me.tomasan7.jecnamobile.ui.component.HorizontalSpacer
import me.tomasan7.jecnamobile.ui.component.ObjectDialog
import me.tomasan7.jecnamobile.ui.component.SubScreenTopAppBar
import me.tomasan7.jecnamobile.ui.component.VerticalSpacer
import me.tomasan7.jecnamobile.ui.component.rememberObjectDialogState
import me.tomasan7.jecnamobile.ui.theme.jm_canteen_disabled
import me.tomasan7.jecnamobile.ui.theme.jm_canteen_ordered
import me.tomasan7.jecnamobile.ui.theme.jm_canteen_ordered_disabled
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.settingsDataStore
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Destination<SubScreensNavGraph>
@Composable
fun CanteenSubScreen(
    navDrawerController: NavDrawerController,
    viewModel: CanteenViewModel = hiltViewModel()
)
{
    val uiState = viewModel.uiState
    val allergensDialogState = rememberObjectDialogState<DayMenu>()
    val helpDialogState = rememberObjectDialogState<Unit>()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var previousTabIndex by remember { mutableIntStateOf(selectedTabIndex) }

    LaunchedEffect(selectedTabIndex) {
        if (previousTabIndex == 1 && selectedTabIndex == 0)
        {
            viewModel.loadMenu()
        }
        else if (previousTabIndex == 0 && selectedTabIndex == 1)
        {
            viewModel.fetchExchange()
        }
        previousTabIndex = selectedTabIndex
    }

    // Pull-to-refresh handled by PullToRefreshBox in the content

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        coroutineScope.launch {
            context.settingsDataStore.data.collect {
                if (!it.canteenHelpSeen)
                {
                    helpDialogState.show(Unit)
                    viewModel.onHelpDialogShownAutomatically()
                }
            }
        }
        onDispose {
            viewModel.leftComposition()
        }
    }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(
                R.string.sidebar_canteen,
                navDrawerController = navDrawerController,
                actions = {
                    if (uiState.credit != null)
                        Credit(uiState.credit)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.loading,
            onRefresh = {
                when (selectedTabIndex) {
                    0 -> viewModel.reloadMenu()
                    1 -> viewModel.reloadExchange()
                }
            },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.zIndex(1f),
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.canteen_menu)) },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.canteen_exchange)) },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when (selectedTabIndex)
                {
                    0 ->
                    {
                        val columnState = remember { LazyListState() }

                        LazyColumn(
                            state = columnState,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            items(uiState.menuSorted, key = { it.day.hashCode() }) { dayMenu ->
                                // TODO: Add animated appearance using AnimatedVisibility
                                DayMenu(
                                    dayMenu = dayMenu,
                                    onMenuItemClick = {
                                        if (it.isOrdered && !it.isEnabled)
                                            viewModel.putMenuItemOnExchange(it, dayMenu.day)
                                        else
                                            viewModel.orderMenuItem(it, dayMenu.day)
                                    },
                                    onInfoClick = { allergensDialogState.show(dayMenu) }
                                )
                            }
                        }

                        InfiniteListHandler(listState = columnState, buffer = 1) {
                            viewModel.loadMoreDayMenus(1)
                        }
                    }
                    1 ->
                    {
                        if (uiState.exchange.isEmpty() && !uiState.loading)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.canteen_exchange_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        else
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxSize()
                            ) {
                                items(uiState.exchange, key = { it.day }) { exchangeDay ->
                                    ExchangeDay(
                                        exchangeDay = exchangeDay,
                                        onItemClick = { viewModel.orderExchangeItem(it) }
                                    )
                                }
                            }
                    }
                }

                // Indicator handled by PullToRefreshBox
            }
            }
        }

        ObjectDialog(
            state = allergensDialogState,
            onDismissRequest = { allergensDialogState.hide() },
            content = { dayMenu ->
                AllergensDialogContent(
                    dayMenu = dayMenu,
                    onCloseCLick = { allergensDialogState.hide() })
            }
        )

        if (uiState.orderInProcess)
            Dialog(onDismissRequest = { }) {
                CircularProgressIndicator()
            }
    }
}

@Composable
private fun ExchangeDay(
    exchangeDay: ExchangeDay,
    modifier: Modifier = Modifier,
    onItemClick: (ExchangeItem) -> Unit,
)
{
    val dayName = getWeekDayName(exchangeDay.day.dayOfWeek)
    val dayDate = remember { exchangeDay.day.format(DATE_FORMATTER) }

    Card(
        title = {
            Text(
                text = "$dayName $dayDate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            exchangeDay.items.getOrNull(0)?.description?.soup?.let { Soup(it) }
            exchangeDay.items.forEach { exchangeItem ->
                ExchangeItem(
                    exchangeItem = exchangeItem,
                    onClick = { onItemClick(exchangeItem) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExchangeItem(
    exchangeItem: ExchangeItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
)
{
    val lunchString = stringResource(R.string.canteen_lunch, exchangeItem.number)
    ElevatedTextRectangle(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onClick() },
            ),
        text = {
            val text = remember(exchangeItem.description?.rest) {
                exchangeItem.description?.rest?.replaceFirstChar { it.uppercase() }
                    ?.replace(" , ", ", ") ?: lunchString
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${exchangeItem.amount}x",
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/* https://dev.to/luismierez/infinite-lazycolumn-in-jetpack-compose-44a4 */
/**
 * Handler to make any lazy column (or lazy row) infinite. Will notify the [onLoadMore]
 * callback once needed
 * @param listState state of the list that needs to also be passed to the LazyColumn composable.
 * Get it by calling rememberLazyListState()
 * @param buffer the number of items before the end of the list to call the onLoadMore callback
 * @param onLoadMore will notify when we need to load more
 */
@Composable
fun InfiniteListHandler(
    listState: LazyListState,
    buffer: Int = 2,
    onLoadMore: () -> Unit
)
{
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .collect {
                onLoadMore()
            }
    }
}

@Composable
private fun DayMenu(
    dayMenu: DayMenu,
    modifier: Modifier = Modifier,
    onInfoClick: () -> Unit = {},
    onMenuItemClick: (MenuItem) -> Unit = {},
    onMenuItemLongClick: (MenuItem) -> Unit = {}
)
{
    val dayName = getWeekDayName(dayMenu.day.dayOfWeek)
    val dayDate = remember { dayMenu.day.format(DATE_FORMATTER) }
    val isSoupSameForAllItems = remember {
        val firstItem = dayMenu.items.firstOrNull() ?: return@remember false
        dayMenu.items.all { it.description?.soup == firstItem.description?.soup }
    }

    Card(
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "$dayName $dayDate",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                )

                val anyMenuItemsWithAllergens = dayMenu.items.any { it.allergens != null }

                if (anyMenuItemsWithAllergens)
                    IconButton(onClick = onInfoClick) {
                        Icon(Icons.Outlined.Info, null)
                    }
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSoupSameForAllItems)
                dayMenu.items.firstOrNull()?.description?.soup?.let { Soup(it) }

            dayMenu.items.forEach { menuItem ->
                key(menuItem) {
                    MenuItem(
                        modifier = Modifier.fillMaxWidth(),
                        menuItem = menuItem,
                        onClick = { onMenuItemClick(menuItem) },
                        onLongClick = { onMenuItemLongClick(menuItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Soup(soup: String)
{
    Surface(
        tonalElevation = ElevationLevel.level3,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_soup_filled),
                contentDescription = null
            )
            Text(soup, Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
private fun Credit(credit: Float)
{
    Icon(
        imageVector = Icons.Filled.AccountBalanceWallet,
        contentDescription = null,
    )

    val creditNumberStr = remember(credit) {
        String.format(if (credit.rem(1) == 0f) "%.0f" else "%.2f", credit)
    }

    Text(
        text = stringResource(R.string.canteen_credit, creditNumberStr),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(10.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuItem(
    menuItem: MenuItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
)
{
    val color = when
    {
        menuItem.isOrdered && !menuItem.isEnabled -> jm_canteen_ordered_disabled
        menuItem.isOrdered                        -> jm_canteen_ordered
        !menuItem.isEnabled                       -> jm_canteen_disabled
        else                                      -> MaterialTheme.colorScheme.surface
    }

    val lunchString = stringResource(R.string.canteen_lunch, menuItem.number)

    val text = remember(menuItem.description?.rest) {
        menuItem.description?.rest?.replaceFirstChar { it.uppercase() }?.replace(" , ", ", ")
            ?: lunchString
    }

    ElevatedTextRectangle(
        text = { Text(text, modifier = Modifier.weight(1f)) },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = color,
        trailingIcon = if (menuItem.isInExchange)
        {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    tint = Color.Gray.copy(alpha = .5f),
                    contentDescription = null
                )
            }
        }
        else
            null
    )
}

@Composable
private fun AllergensDialogContent(
    dayMenu: DayMenu,
    onCloseCLick: () -> Unit = {}
)
{
    DialogContainer(
        title = {
            Text(stringResource(R.string.canteen_allergens))
        },
        buttons = {
            TextButton(onClick = onCloseCLick) {
                Text(stringResource(R.string.close))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dayMenu.items.forEach { menuItem ->
                AllergensForMenuItem(menuItem)
            }
        }
    }
}

@Composable
private fun AllergensForMenuItem(menuItem: MenuItem)
{
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.canteen_lunch, menuItem.number))

        ElevatedTextRectangle(
            modifier = Modifier.fillMaxWidth()
        ) {
            val allergensText = remember(menuItem.allergens) {
                menuItem.allergens?.joinToString { it.split(" - ")[0] }
            }

            Text(
                text = allergensText ?: "",
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ElevatedTextRectangle(
    modifier: Modifier = Modifier,
    elevation: Dp = ElevationLevel.level5,
    color: Color = MaterialTheme.colorScheme.surface,
    trailingIcon: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    text: @Composable RowScope.() -> Unit
)
{
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = elevation,
        color = color,
    ) {
        Column(modifier = Modifier.padding(top = 5.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)) {
            if (label != null)
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelSmall) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        label()
                    }
                }
            VerticalSpacer(5.dp)
            Row {
                text()
                if (trailingIcon != null)
                {
                    HorizontalSpacer(10.dp)
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
private fun ElevatedTextRectangle(
    modifier: Modifier = Modifier,
    elevation: Dp = ElevationLevel.level5,
    color: Color = MaterialTheme.colorScheme.surface,
    trailingIcon: ImageVector? = null,
    text: String
) = ElevatedTextRectangle(
    modifier = modifier,
    elevation = elevation,
    color = color,
    trailingIcon = trailingIcon?.let { { Icon(it, null) } },
    text = { Text(text, modifier = Modifier.weight(1f)) }
)

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.")
