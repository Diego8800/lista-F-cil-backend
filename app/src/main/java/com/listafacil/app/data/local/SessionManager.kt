package com.listafacil.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "listafacil_session")

/**
 * Sessão e preferências locais:
 * - token de sessão: criptografado (AES/GCM + Android Keystore);
 * - tema: "LIGHT" | "DARK".
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("session_token")
        val THEME = stringPreferencesKey("app_theme")
    }

    val token: Flow<String?> = context.sessionDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[Keys.TOKEN]?.let { runCatching { KeystoreAes.decrypt(it) }.getOrNull() }
        }

    val theme: Flow<String> = context.sessionDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.THEME] ?: "LIGHT" }

    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { it[Keys.TOKEN] = KeystoreAes.encrypt(token) }
    }

    suspend fun clearToken() {
        context.sessionDataStore.edit { it.remove(Keys.TOKEN) }
    }

    suspend fun saveTheme(theme: String) {
        context.sessionDataStore.edit { it[Keys.THEME] = theme }
    }
}
