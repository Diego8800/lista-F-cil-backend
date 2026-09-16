package com.listafacil.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareProductsUseCaseTest {

    private val useCase = CompareProductsUseCase()

    @Test
    fun `pacote maior vence quando custo por grama e menor`() {
        val result = useCase(
            listOf(
                ComparisonInput("Arroz 5kg", 2500, 5.0, "kg"),
                ComparisonInput("Arroz 1kg", 600, 1.0, "kg")
            )
        )
        assertTrue(result.compatible)
        assertEquals(0, result.bestIndex) // 5 cents/g vs 6 cents/g
    }

    @Test
    fun `conversao kg para g normaliza corretamente`() {
        val result = useCase(
            listOf(
                ComparisonInput("Farinha 2kg", 1000, 2.0, "kg"),   // 0,5 cent/g
                ComparisonInput("Farinha 500g", 300, 500.0, "g")   // 0,6 cent/g
            )
        )
        assertTrue(result.compatible)
        assertEquals("g", result.products[0].baseUnit)
        assertEquals(2000.0, result.products[0].normalizedQuantity, 0.001)
        assertEquals(0, result.bestIndex)
    }

    @Test
    fun `conversao L para ml normaliza corretamente`() {
        val result = useCase(
            listOf(
                ComparisonInput("Refri 2L", 800, 2.0, "L"),
                ComparisonInput("Refri 600ml", 300, 600.0, "ml")
            )
        )
        assertTrue(result.compatible)
        assertEquals("ml", result.products[0].baseUnit)
        assertEquals(2000.0, result.products[0].normalizedQuantity, 0.001)
    }

    @Test
    fun `unidades incompativeis bloqueiam comparacao`() {
        val result = useCase(
            listOf(
                ComparisonInput("Carne 1kg", 4000, 1.0, "kg"),
                ComparisonInput("Leite 1L", 500, 1.0, "L")
            )
        )
        assertFalse(result.compatible)
        assertNull(result.bestIndex)
    }

    @Test
    fun `unidade un compara diretamente`() {
        val result = useCase(
            listOf(
                ComparisonInput("Pão 10un", 1500, 10.0, "un"),
                ComparisonInput("Pão 5un", 800, 5.0, "un")
            )
        )
        assertTrue(result.compatible)
        assertEquals(0, result.bestIndex) // 150/un vs 160/un
    }

    @Test
    fun `menos de dois produtos falha`() {
        val result = useCase(listOf(ComparisonInput("Só um", 100, 1.0, "un")))
        assertFalse(result.compatible)
    }

    @Test
    fun `quantidade ou preco invalidos falham`() {
        val result = useCase(
            listOf(
                ComparisonInput("A", 100, 0.0, "un"),
                ComparisonInput("B", 100, 1.0, "un")
            )
        )
        assertFalse(result.compatible)
    }
}
