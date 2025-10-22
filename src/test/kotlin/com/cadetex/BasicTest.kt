package com.cadetex

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BasicTest {
    
    @Test
    fun `should pass basic test`() {
        assertEquals(1 + 1, 2)
    }
    
    @Test
    fun `should test string concatenation`() {
        val result = "Hello" + " " + "World"
        assertEquals("Hello World", result)
    }
}
