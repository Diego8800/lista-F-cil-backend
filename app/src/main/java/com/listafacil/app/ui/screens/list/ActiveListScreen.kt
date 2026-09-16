package com.listafacil.app.ui.screens.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.core.Money
import com.listafacil.app.domain.model.ShoppingItem

@Composable
fun ActiveListScreen(
    onAddItem: (String) -> Unit,
    onEditItem: (String, String) -> Unit,
    onViewHistory: (String) -> Unit,
    onFinished: () -> Unit,
    viewModel: ActiveListViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.finishedMessage) {
        ui.finishedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeFinishedMessage()
            onFinished()
        }
    }
    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val list = ui.list
    val remaining = viewModel.remainingCents
    val budget = list?.budgetCents ?: 0L

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Painel do Budget (requisito §7: budget disponível e valor restante)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(list?.name ?: "Lista", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Budget:", style = MaterialTheme.typography.bodyMedium)
                        Text(Money.format(budget), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Restante:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Money.format(remaining),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (remaining < 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (budget > 0) {
                        val progress = ((budget - remaining).toFloat() / budget).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(
                        onClick = { viewModel.finish(null) },
                        enabled = !ui.finishing && ui.items.isNotEmpty(),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text(if (ui.finishing) "Finalizando..." else "FINALIZAR LISTA") }
                }
            }

            if (ui.items.isEmpty() && !ui.loading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Lista vazia", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Toque em + para adicionar o primeiro produto.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onToggle = { viewModel.togglePurchased(item) },
                        onEdit = { onEditItem(item.listId, item.id) },
                        onDelete = { viewModel.deleteItem(item) },
                        onHistory = { onViewHistory(item.productId) }
                    )
                }
            }
        }

        // FAB e Snackbar ancorados na parte inferior, sobre o conteúdo
        ExtendedFloatingActionButton(
            onClick = { list?.let { onAddItem(it.id) } },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Produto") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (ui.showNonPurchasedDialog) {
        NonPurchasedDialog(
            count = ui.nonPurchasedCount,
            onDismiss = viewModel::dismissNonPurchasedDialog,
            onDelete = { viewModel.finish("delete") },
            onTransfer = { viewModel.finish("transfer") }
        )
    }
}

@Composable
private fun ItemCard(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (item.purchased) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.purchased, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.categoryName} · ${Money.formatQuantity(item.quantity)} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall
                )
                item.previousPriceCents?.let {
                    Text(
                        "Anterior: ${Money.format(it)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    if (item.currentPriceCents != null) {
                        "Atual: ${Money.format(item.currentPriceCents)}"
                    } else {
                        "Preço não informado"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.currentPriceCents != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                item.establishmentName?.let {
                    Text("Estabelecimento: $it", style = MaterialTheme.typography.bodySmall)
                }
                item.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onHistory) {
                Icon(Icons.Default.History, contentDescription = "Histórico")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir")
            }
        }
    }
}

/** Pop-up: "O que deseja fazer com os produtos não comprados?" (requisito §8) */
@Composable
private fun NonPurchasedDialog(
    count: Int,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onTransfer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalizar lista") },
        text = { Text("Existem $count produto(s) não comprado(s). O que deseja fazer com eles?") },
        confirmButton = {
            Column {
                TextButton(onClick = onTransfer) { Text("Transferir para nova lista") }
                TextButton(onClick = onDelete) { Text("Excluir não comprados") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Continuar editando") }
        }
    )
}
