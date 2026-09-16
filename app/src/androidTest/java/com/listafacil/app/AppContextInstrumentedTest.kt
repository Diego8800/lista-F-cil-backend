package com.listafacil.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste instrumentado: valida o pacote do app instalado no dispositivo/emulador.
 * Roda com: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class AppContextInstrumentedTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.listafacil.app", appContext.packageName)
    }
}
