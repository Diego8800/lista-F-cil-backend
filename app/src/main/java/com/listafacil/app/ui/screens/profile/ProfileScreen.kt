package com.listafacil.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.ui.AppViewModel
import com.listafacil.app.ui.screens.auth.AuthViewModel

@Composable
fun ProfileScreen(
    onThemeChange: (Boolean) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel()
) {
    val authUi by authViewModel.ui.collectAsState()
    val appUi by appViewModel.ui.collectAsState()

    var name by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dados cadastrais
        Text("Dados cadastrais", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nome") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { authViewModel.updateName(name) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Salvar nome") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Alteração de senha
        Text("Alterar senha", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = currentPassword, onValueChange = { currentPassword = it },
            label = { Text("Senha atual") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPassword, onValueChange = { newPassword = it },
            label = { Text("Nova senha (mín. 8 caracteres)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { authViewModel.changePassword(currentPassword, newPassword) },
            enabled = currentPassword.isNotBlank() && newPassword.length >= 8,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Alterar senha") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Tema
        Text("Tema do aplicativo", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !appUi.darkTheme,
                onClick = { onThemeChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Claro") }
            SegmentedButton(
                selected = appUi.darkTheme,
                onClick = { onThemeChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Escuro") }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        authUi.successMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        authUi.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedButton(
            onClick = { authViewModel.logout() },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sair da conta") }
    }
}
