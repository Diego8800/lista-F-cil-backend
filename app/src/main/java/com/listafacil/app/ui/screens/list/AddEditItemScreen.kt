package com.listafacil.app.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.listafacil.app.core.Money
import com.listafacil.app.core.SUPPORTED_UNITS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    onBack: () -> Unit,
    viewModel: AddEditItemViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val editingItem = ui.item

    var productName by remember { mutableStateOf(editingItem?.productName ?: "") }
    var categoryId by remember { mutableStateOf(editingItem?.categoryId) }
    var categoryQuery by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(editingItem?.quantity?.toString() ?: "1") }
    var unit by remember { mutableStateOf(editingItem?.unit ?: "un") }
    var currentPrice by remember {
        mutableStateOf(editingItem?.currentPriceCents?.let { Money.format(it).replace("R$", "").trim() } ?: "")
    }
    var note by remember { mutableStateOf(editingItem?.note ?: "") }
    var establishmentId by remember { mutableStateOf(editingItem?.establishmentId) }
    var establishmentQuery by remember { mutableStateOf("") }

    var productMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var unitMenu by remember { mutableStateOf(false) }
    var establishmentMenu by remember { mutableStateOf(false) }
    var newCategoryDialog by remember { mutableStateOf(false) }
    var newEstablishmentDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadEstablishments()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }
    LaunchedEffect(editingItem) {
        editingItem?.let {
            if (productName.isEmpty()) {
                productName = it.productName
                categoryId = it.categoryId
                categoryQuery = ui.categories.firstOrNull { c -> c.id == it.categoryId }?.name ?: ""
                quantity = it.quantity.toString()
                unit = it.unit
                currentPrice = it.currentPriceCents?.let { c -> Money.format(c).replace("R$", "").trim() } ?: ""
                note = it.note ?: ""
                establishmentId = it.establishmentId
                establishmentQuery = it.establishmentName ?: ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ui.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        // Nome do produto com autocomplete
        ExposedDropdownMenuBox(
            expanded = productMenu && ui.productSuggestions.isNotEmpty(),
            onExpandedChange = { productMenu = it }
        ) {
            OutlinedTextField(
                value = productName,
                onValueChange = {
                    productName = it
                    viewModel.onProductQueryChange(it)
                    productMenu = true
                },
                label = { Text("Nome do produto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(
                expanded = productMenu && ui.productSuggestions.isNotEmpty(),
                onDismissRequest = { productMenu = false }
            ) {
                ui.productSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            productName = suggestion
                            viewModel.onProductQueryChange("")
                            productMenu = false
                        }
                    )
                }
            }
        }

        // Categoria com autocomplete + criar inline
        ExposedDropdownMenuBox(
            expanded = categoryMenu,
            onExpandedChange = { categoryMenu = it }
        ) {
            OutlinedTextField(
                value = categoryQuery,
                onValueChange = {
                    categoryQuery = it
                    categoryMenu = true
                },
                label = { Text("Categoria") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryMenu) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(
                expanded = categoryMenu,
                onDismissRequest = { categoryMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("+ Nova categoria") },
                    onClick = {
                        categoryMenu = false
                        newCategoryDialog = true
                    }
                )
                val filtered = ui.categories.filter {
                    it.name.contains(categoryQuery, ignoreCase = true)
                }
                filtered.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            categoryId = category.id
                            categoryQuery = category.name
                            categoryMenu = false
                        }
                    )
                }
            }
        }

        RowOfFields(
            quantity = quantity,
            onQuantityChange = { quantity = it },
            unit = unit,
            unitMenu = unitMenu,
            onUnitMenuChange = { unitMenu = it },
            onUnitSelect = { unit = it }
        )

        OutlinedTextField(
            value = editingItem?.previousPriceCents?.let { Money.format(it) } ?: "Sem histórico",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Preço anterior") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = currentPrice,
            onValueChange = { currentPrice = it },
            label = { Text("Preço atual (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Estabelecimento com autocomplete + criar inline
        ExposedDropdownMenuBox(
            expanded = establishmentMenu,
            onExpandedChange = { establishmentMenu = it }
        ) {
            OutlinedTextField(
                value = establishmentQuery,
                onValueChange = {
                    establishmentQuery = it
                    establishmentId = null
                    establishmentMenu = true
                },
                label = { Text("Estabelecimento (opcional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(establishmentMenu) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(
                expanded = establishmentMenu,
                onDismissRequest = { establishmentMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("+ Novo estabelecimento") },
                    onClick = {
                        establishmentMenu = false
                        newEstablishmentDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Nenhum") },
                    onClick = {
                        establishmentId = null
                        establishmentQuery = ""
                        establishmentMenu = false
                    }
                )
                val filtered = ui.establishments.filter {
                    it.name.contains(establishmentQuery, ignoreCase = true)
                }
                filtered.forEach { est ->
                    DropdownMenuItem(
                        text = { Text(est.name) },
                        onClick = {
                            establishmentId = est.id
                            establishmentQuery = est.name
                            establishmentMenu = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Observação (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                viewModel.save(
                    productName = productName,
                    categoryId = categoryId,
                    quantity = quantity.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    unit = unit,
                    currentPriceCents = Money.parseToCents(currentPrice),
                    note = note,
                    establishmentId = establishmentId
                )
            },
            enabled = !ui.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (ui.saving) "Salvando..." else if (ui.isEditing) "Salvar alterações" else "Adicionar à lista")
        }
    }

    if (newCategoryDialog) {
        var newName by remember { mutableStateOf(categoryQuery) }
        AlertDialog(
            onDismissRequest = { newCategoryDialog = false },
            title = { Text("Nova categoria") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        viewModel.addCategory(newName)
                        categoryQuery = newName
                        newCategoryDialog = false
                    }
                ) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { newCategoryDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (newEstablishmentDialog) {
        var newName by remember { mutableStateOf(establishmentQuery) }
        AlertDialog(
            onDismissRequest = { newEstablishmentDialog = false },
            title = { Text("Novo estabelecimento") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        viewModel.addEstablishment(newName)
                        establishmentQuery = newName
                        newEstablishmentDialog = false
                    }
                ) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { newEstablishmentDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowOfFields(
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unit: String,
    unitMenu: Boolean,
    onUnitMenuChange: (Boolean) -> Unit,
    onUnitSelect: (String) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantidade") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        ExposedDropdownMenuBox(
            expanded = unitMenu,
            onExpandedChange = onUnitMenuChange,
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = unit,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unidade") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = unitMenu,
                onDismissRequest = { onUnitMenuChange(false) }
            ) {
                SUPPORTED_UNITS.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(u) },
                        onClick = {
                            onUnitSelect(u)
                            onUnitMenuChange(false)
                        }
                    )
                }
            }
        }
    }
}
