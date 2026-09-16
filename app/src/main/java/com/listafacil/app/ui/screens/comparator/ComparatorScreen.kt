package com.listafacil.app.ui.screens.comparator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.core.SUPPORTED_UNITS
import com.listafacil.app.domain.usecase.ComparisonResult
import com.listafacil.app.domain.usecase.UnitPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparatorScreen(viewModel: ComparatorViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Compare produtos equivalentes com diferentes tamanhos e descubra o melhor custo por unidade.",
            style = MaterialTheme.typography.bodyMedium
        )

        ui.products.forEachIndexed { index, input ->
            ComparatorProductCard(
                index = index,
                input = input,
                canRemove = ui.products.size > 2,
                onChange = { viewModel.update(index, it) },
                onRemove = { viewModel.removeAt(index) }
            )
        }

        TextButton(onClick = { viewModel.addProduct() }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Adicionar produto")
        }

        ui.inputError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(onClick = { viewModel.compare() }, modifier = Modifier.fillMaxWidth()) {
            Text("Comparar")
        }

        ui.result?.let { result ->
            ComparisonResultContent(result)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComparatorProductCard(
    index: Int,
    input: ComparatorProductInput,
    canRemove: Boolean,
    onChange: (ComparatorProductInput) -> Unit,
    onRemove: () -> Unit
) {
    var unitMenu by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Produto ${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Text("✕", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            OutlinedTextField(
                value = input.name,
                onValueChange = { onChange(input.copy(name = it)) },
                label = { Text("Nome (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input.price,
                    onValueChange = { onChange(input.copy(price = it)) },
                    label = { Text("Preço (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = input.quantity,
                    onValueChange = { onChange(input.copy(quantity = it)) },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            ExposedDropdownMenuBox(expanded = unitMenu, onExpandedChange = { unitMenu = it }) {
                OutlinedTextField(
                    value = input.unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unidade") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                    SUPPORTED_UNITS.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u) },
                            onClick = {
                                onChange(input.copy(unit = u))
                                unitMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonResultContent(result: ComparisonResult) {
    val pricePerBase = remember { com.listafacil.app.domain.usecase.CompareProductsUseCase() }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!result.compatible) {
                Text(
                    result.message ?: "Não foi possível comparar.",
                    color = MaterialTheme.colorScheme.error
                )
                return@Column
            }

            Text("Preço por unidade de medida", style = MaterialTheme.typography.titleSmall)
            result.products.forEachIndexed { index, product ->
                val isBest = index == result.bestIndex
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isBest) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${product.quantity} ${product.unit} → ${pricePerBase.pricePerBaseDisplay(product)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isBest) {
                        Text(
                            "✓ Melhor custo",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            result.bestIndex?.let {
                Text(
                    "Melhor custo-benefício: ${result.products[it].name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
