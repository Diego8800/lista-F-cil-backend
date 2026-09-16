package com.listafacil.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.core.Money

@Composable
fun PriceHistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsState()
    val history = ui.history

    if (history == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (ui.loading) Text("Carregando...")
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(history.productName, style = MaterialTheme.typography.headlineSmall)

        // Estatísticas (requisito §9)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatRow("Preço atual", history.stats.currentCents?.let { Money.format(it) } ?: "—")
                StatRow("Preço anterior", history.stats.previousCents?.let { Money.format(it) } ?: "—")
                StatRow("Menor preço", history.stats.minCents?.let { Money.format(it) } ?: "—")
                StatRow("Maior preço", history.stats.maxCents?.let { Money.format(it) } ?: "—")
                StatRow(
                    "Preço médio",
                    history.stats.avgCents?.let { Money.format(it.toLong()) } ?: "—"
                )
            }
        }

        Text("Histórico cronológico", style = MaterialTheme.typography.titleMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history.history, key = { it.id }) { record ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(Money.format(record.priceCents), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${Money.formatQuantity(record.quantity)} ${record.unit}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            record.establishmentName?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            Money.formatDate(record.recordedAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
