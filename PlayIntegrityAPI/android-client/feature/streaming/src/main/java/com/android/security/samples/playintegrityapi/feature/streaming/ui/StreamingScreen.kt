package com.android.security.samples.playintegrityapi.feature.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import com.android.security.samples.playintegrityapi.core.ui.components.LoadableButton
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.streaming.R

data class StreamingTier(
    val title: String,
    val description: String,
    val isActive: Boolean = false,
    val hasWarning: Boolean = false
)

@Composable
fun StreamingRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StreamingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    StreamingScreen(
        onBackClick = onBackClick,
        onFetchManifestClick = viewModel::fetchManifest,
        exoPlayer = viewModel.exoPlayer,
        uiState = uiState,
        onLifecycleStop = viewModel::onLifecycleStop,
        onLifecycleStart = viewModel::onLifecycleStart,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingScreen(
    onBackClick: () -> Unit,
    onFetchManifestClick: () -> Unit,
    exoPlayer: ExoPlayer?,
    uiState: StreamingUiState,
    onLifecycleStop: () -> Unit,
    onLifecycleStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tiers = listOf(
        StreamingTier(
            title = stringResource(id = R.string.streaming_tier_premium_title),
            description = stringResource(id = R.string.streaming_tier_premium_desc),
            isActive = uiState.activeTierIndex == 0
        ), StreamingTier(
            title = stringResource(id = R.string.streaming_tier_high_title),
            description = stringResource(id = R.string.streaming_tier_high_desc),
            isActive = uiState.activeTierIndex == 1
        ), StreamingTier(
            title = stringResource(id = R.string.streaming_tier_standard_title),
            description = stringResource(id = R.string.streaming_tier_standard_desc),
            isActive = uiState.activeTierIndex == 2
        ), StreamingTier(
            title = stringResource(id = R.string.streaming_tier_basic_title),
            description = stringResource(id = R.string.streaming_tier_basic_desc),
            isActive = uiState.activeTierIndex == 3
        ), StreamingTier(
            title = stringResource(id = R.string.streaming_tier_restricted_title),
            description = stringResource(id = R.string.streaming_tier_restricted_desc),
            isActive = uiState.activeTierIndex == 4,
            hasWarning = true
        )
    )

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.activeTierIndex) {
        if (uiState.activeTierIndex != -1) {
            listState.animateScrollToItem(uiState.activeTierIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.streaming_top_bar_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                            )
                        )
                    }, navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, contentDescription =
                                    stringResource(com.android.security.samples.playintegrityapi.core.ui.R.string.navigate_back_content_desc)
                            )
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }
        }, containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black, MaterialTheme.shapes.large)
                    .border(
                        1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large
                    )
                    .clip(MaterialTheme.shapes.large)
            ) {
                VideoPlayerWidget(
                    exoPlayer = exoPlayer,
                    state = uiState.playerState,
                    onRetry = onFetchManifestClick,
                    onLifecycleStop = onLifecycleStop,
                    onLifecycleStart = onLifecycleStart
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.streaming_server_response),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tiers.size) { index -> TierCard(tier = tiers[index]) }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    LoadableButton(
                        onClick = onFetchManifestClick,
                        isLoading = uiState.isRefreshing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(id = R.string.streaming_btn_fetch_manifest),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TierCard(tier: StreamingTier, modifier: Modifier = Modifier) {
    val isActive = tier.isActive
    val cardAlpha = if (isActive) 1f else 0.4f

    val simulatedTranslucentColor = MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = 0.3f)
        .compositeOver(MaterialTheme.colorScheme.background)

    val containerColor = if (isActive) {
        simulatedTranslucentColor
    } else {
        Color.Transparent
    }

    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val borderWidth = if (isActive) 2.dp else 1.dp
    val shadowElevation = if (isActive) 5.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .border(borderWidth, borderColor, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(cardAlpha),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tier.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tier.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (tier.hasWarning) {
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun StreamingScreenPreview_PremiumActive() {
    PiaSampleTheme(dynamicColor = false) {
        StreamingScreen(
            onBackClick = {},
            onFetchManifestClick = {},
            exoPlayer = null,
            uiState = StreamingUiState(
                activeTierIndex = 0,
                isInitialLoading = false,
                isRefreshing = false,
                playerState = VideoPlayerUiState(
                    isLoading = false,
                    isError = false,
                    isPlaying = false,
                    errorMessage = null
                )
            ),
            onLifecycleStop = {},
            onLifecycleStart = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun StreamingScreenPreview_RestrictedActive() {
    PiaSampleTheme(dynamicColor = false) {
        StreamingScreen(
            onBackClick = {},
            onFetchManifestClick = {},
            exoPlayer = null,
            uiState = StreamingUiState(
                activeTierIndex = 4,
                isInitialLoading = false,
                isRefreshing = false,
                playerState = VideoPlayerUiState(
                    isLoading = false,
                    isError = false,
                    isPlaying = false,
                    errorMessage = null
                )
            ),
            onLifecycleStop = {},
            onLifecycleStart = {}
        )
    }
}