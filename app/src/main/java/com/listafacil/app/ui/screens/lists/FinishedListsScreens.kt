package com.listafacil.app.ui.screens.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.core.Money
import com.listafacil.app.domain.model.ShoppingItem

@Composable
fun FinishedListsScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: FinishedListsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        when {
            ui.loading -> Text("Carregando...")
            ui.error != null -> Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            ui.lists.isEmpty() -> Text(
                "Nenhuma lista finalizada ainda.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.lists, key = { it.id }) { list ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDetail(list.id) }
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(list.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Finalizada em ${Money.formatDate(list.finishedAt)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(Money.format(list.spentCents), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun FinishedListDetailScreen(
    onOpenReports: () -> Unit,
    viewModel: FinishedDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val list = ui.list ?: return

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(list.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Finalizada em ${Money.formatDate(list.finishedAt)} · Total: ${Money.format(list.spentCents)}",
            style = MaterialTheme.typography.bodyMedium
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(ui.items, key = { it.id }) { item ->
                FinishedItemRow(item)
            }
        }

        Button(onClick = onOpenReports, modifier = Modifier.fillMaxWidth()) {
            Text("Ver relatórios relacionados")
        }
    }
}

@Composable
private fun FinishedItemRow(item: ShoppingItem) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.purchased) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.purchased) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "${Money.formatQuantity(item.quantity)} ${item.unit}" +
                        if (item.establishmentName != null) " · ${item.establishmentName}" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (item.purchased) {
                    Text(Money.format(item.currentPriceCents), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Não comprado", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
