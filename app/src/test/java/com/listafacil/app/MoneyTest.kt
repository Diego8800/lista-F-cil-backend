package com.listafacil.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parse inteiro vira centavos`() {
        assertEquals(1000L, Money.parseToCents("10"))
    }

    @Test
    fun `parse com virgula`() {
        assertEquals(1050L, Money.parseToCents("10,50"))
    }

    @Test
    fun `parse com ponto`() {
        assertEquals(1050L, Money.parseToCents("10.50"))
    }

    @Test
    fun `parse com simbolo e milhar`() {
        assertEquals(123456L, Money.parseToCents("R$ 1.234,56"))
    }

    @Test
    fun `parse invalido retorna null`() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("-5"))
    }

    @Test
    fun `formato moeda BRL`() {
        assertEquals("R$ 10,50", Money.format(1050))
    }

    @Test
    fun `formato data ISO para BR`() {
        assertEquals("13/09/2026", Money.formatDate("2026-09-13T18:30:00.000Z"))
    }
}
