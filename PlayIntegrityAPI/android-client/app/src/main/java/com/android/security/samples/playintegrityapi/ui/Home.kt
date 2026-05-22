package com.android.security.samples.playintegrityapi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.security.samples.playintegrityapi.R
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.core.ui.theme.Slate400

@Composable
fun HomeRoute(
    onNavigateToBank: () -> Unit,
    onNavigateToStreaming: () -> Unit,
    onNavigateToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeScreen(
        onNavigateToBank = onNavigateToBank,
        onNavigateToStreaming = onNavigateToStreaming,
        onNavigateToGame = onNavigateToGame,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBank: () -> Unit,
    onNavigateToStreaming: () -> Unit,
    onNavigateToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.home_top_bar_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate400
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_header_select_use_case),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Bank Micro-App Card
                UseCaseCard(
                    icon = Icons.Outlined.AccountBalance,
                    title = stringResource(id = R.string.use_case_bank_title),
                    description = stringResource(id = R.string.use_case_bank_desc),
                    onClick = onNavigateToBank
                )

                // Streaming Micro-App Card
                UseCaseCard(
                    icon = Icons.Outlined.PlayCircleOutline,
                    title = stringResource(id = R.string.use_case_streaming_title),
                    description = stringResource(id = R.string.use_case_streaming_desc),
                    onClick = onNavigateToStreaming
                )

                // Game Micro-App Card
                UseCaseCard(
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(id = R.string.use_case_game_title),
                    description = stringResource(id = R.string.use_case_game_desc),
                    onClick = onNavigateToGame
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            StatusPill(
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    // Temporarily disable status pill until it's able to accurately reflect
                    // the state of the connection between the client and server and the device
                    // integrity
                    .alpha(0f)
            )
        }
    }
}

@Composable
fun UseCaseCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(0.6f)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.use_case_navigate_content_desc),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun StatusPill(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Text(
                text = stringResource(id = R.string.home_status_integrity_secure),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
internal fun HomeScreenPreview() {
    PiaSampleTheme(dynamicColor = false) {
        HomeScreen(
            onNavigateToBank = {},
            onNavigateToStreaming = {},
            onNavigateToGame = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
internal fun UseCaseCardPreview() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            UseCaseCard(
                icon = Icons.Outlined.AccountBalance,
                title = stringResource(id = R.string.use_case_bank_title),
                description = stringResource(id = R.string.use_case_bank_desc),
                onClick = {}
            )
        }
    }
}