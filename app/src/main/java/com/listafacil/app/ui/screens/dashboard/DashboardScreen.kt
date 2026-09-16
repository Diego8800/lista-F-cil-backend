package com.listafacil.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.core.Money

@Composable
fun DashboardScreen(
    onOpenActiveList: (String) -> Unit,
    onOpenFinishedLists: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenComparator: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    var showNewListDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ui.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        // Lista ativa
        val active = ui.activeList
        if (active != null) {
            Card(
                onClick = { onOpenActiveList(active.id) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Lista ativa", style = MaterialTheme.typography.labelLarge)
                    Text(active.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Restante: ${Money.format(active.budgetCents - active.spentCents)} de ${Money.format(active.budgetCents)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Ações principais
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(
                title = "Nova Lista",
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                enabled = active == null,
                modifier = Modifier.weight(1f)
            ) { showNewListDialog = true }
            DashboardCard(
                title = "Últimas Listas",
                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                modifier = Modifier.weight(1f)
            ) { onOpenFinishedLists() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(
                title = "Relatórios",
                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.weight(1f)
            ) { onOpenReports() }
            DashboardCard(
                title = "Comparador",
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f)
            ) { onOpenComparator() }
        }
        DashboardCard(
            title = "Perfil",
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        ) { onOpenProfile() }

        if (ui.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }

    if (showNewListDialog) {
        NewListDialog(
            loading = ui.creatingList,
            onDismiss = { showNewListDialog = false },
            onCreate = { name, budgetCents ->
                viewModel.createList(name, budgetCents) { listId ->
                    showNewListDialog = false
                    onOpenActiveList(listId)
                }
            }
        )
    }
}

@Composable
private fun DashboardCard(
    title: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(onClick = onClick, enabled = enabled, modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NewListDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nome da lista") }, singleLine = true
                )
                OutlinedTextField(
                    value = budget, onValueChange = { budget = it },
                    label = { Text("Budget (R$)") }, singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    val cents = Money.parseToCents(budget) ?: 0L
                    onCreate(name, cents)
                }
            ) { Text(if (loading) "Criando..." else "Criar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
