package com.listafacil.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.listafacil.app.ui.navigation.AuthNavHost
import com.listafacil.app.ui.navigation.MainNavHost
import com.listafacil.app.ui.theme.ListaFacilTheme

@Composable
fun ListaFacilRoot(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.ui.collectAsState()

    ListaFacilTheme(darkTheme = state.darkTheme) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.authenticated -> MainNavHost(onThemeChange = viewModel::setDarkTheme)
            else -> AuthNavHost()
        }
    }
}
