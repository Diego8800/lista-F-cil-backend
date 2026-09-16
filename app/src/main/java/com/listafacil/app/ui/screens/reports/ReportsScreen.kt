package com.listafacil.app.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.core.Money
import com.listafacil.app.domain.model.Evolution
import com.listafacil.app.domain.model.TopChange

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportType.entries.forEach { type ->
            Card(
                onClick = { viewModel.select(type) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    type.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        ui.selected?.let { selected ->
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(selected.label, style = MaterialTheme.typography.titleMedium)
            when {
                ui.loading -> Text("Carregando...")
                ui.error != null -> Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                else -> ReportContent(ui)
            }
        }
    }
}

@Composable
private fun ReportContent(ui: ReportsUiState) {
    // Coluna com altura limitada dentro do scroll da tela — sem LazyColumn aninhada.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val empty = ui.rows.isEmpty() && ui.topChanges.isEmpty() && ui.evolutions.isEmpty()
        if (empty) {
            Text("Sem dados registrados para este relatório.", style = MaterialTheme.typography.bodyMedium)
        }

        ui.rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    row.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(row.valueCents?.let { Money.format(it) } ?: "—")
            }
        }

        ui.topChanges.forEach { change ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${change.productName} (${change.unit})", modifier = Modifier.weight(1f))
                Text(
                    "${Money.format(change.firstCents)} → ${Money.format(change.lastCents)} " +
                        "(${Money.formatPct(change.changePct)})",
                    color = if (change.changePct > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        ui.evolutions.forEach { evolution ->
            EvolutionBlock(evolution)
        }

        ui.totalCents?.let {
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total economizado", style = MaterialTheme.typography.titleSmall)
                Text(Money.format(it), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun EvolutionBlock(evolution: Evolution) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(evolution.productName, style = MaterialTheme.typography.titleSmall)
        // últimos 10 registros por produto para manter a tela legível
        evolution.points.takeLast(10).forEach { point ->
            Text(
                "  ${Money.formatDate(point.recordedAt)}: ${Money.format(point.priceCents)}" +
                    (point.establishmentName?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (evolution.points.size > 10) {
            Text(
                "  … e mais ${evolution.points.size - 10} registro(s)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
