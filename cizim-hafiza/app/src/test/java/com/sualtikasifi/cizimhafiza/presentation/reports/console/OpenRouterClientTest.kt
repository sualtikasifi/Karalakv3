package com.sualtikasifi.cizimhafiza.presentation.reports.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OpenRouterClientTest {
    private val client = OpenRouterClient("test")

    @Test
    fun readsTextCostAndCitations() {
        val raw = """
            {"id":"gen-1","model":"google/gemini-3.1-flash-lite","choices":[{"finish_reason":"stop",
             "message":{"role":"assistant","content":"## Plan\n- bir","annotations":[
               {"type":"url_citation","url_citation":{"url":"https://a.example","title":"A","start_index":0,"end_index":4}},
               {"type":"url_citation","url_citation":{"url":"https://a.example","title":"A again"}},
               {"type":"url_citation","url_citation":{"url":"https://b.example","title":""}}]}}],
             "usage":{"prompt_tokens":3100,"completion_tokens":2400,"total_tokens":5500,"cost":0.004375}}
        """.trimIndent()
        val a = client.parse(raw, "asked/model", search = true)
        assertEquals("## Plan\n- bir", a.text)
        assertEquals("google/gemini-3.1-flash-lite", a.model)
        assertEquals(listOf("A" to "https://a.example", "https://b.example" to "https://b.example"), a.sources)
        assertTrue(a.searched)
        assertEquals(0.004375, a.costUsd!!, 1e-9)
        assertEquals(3100, a.promptTokens)
        assertEquals(2400, a.completionTokens)
    }

    @Test
    fun missingUsageLeavesCostUnknown() {
        val a = client.parse("""{"choices":[{"message":{"content":"tamam"}}]}""", "m", search = false)
        assertEquals("tamam", a.text)
        assertEquals("m", a.model)
        assertNull(a.costUsd)
    }

    @Test
    fun anErrorInsideA200IsAFailure() {
        try {
            client.parse("""{"choices":[{"error":{"code":502,"message":"upstream down"},"message":{"content":""}}]}""", "m", search = false)
            fail("expected an error")
        } catch (e: OpenRouterClient.ApiError) {
            assertEquals(502, e.code)
        }
    }

    @Test
    fun anEmptyAnswerIsAFailure() {
        try {
            client.parse("""{"choices":[{"finish_reason":"length","message":{"content":"  "}}]}""", "m", search = false)
            fail("expected an error")
        } catch (e: OpenRouterClient.ApiError) {
            assertTrue(e.message!!.contains("length"))
        }
    }

    @Test
    fun dollarsStayReadableBelowACent() {
        assertEquals("$0.0044", OpenRouterClient.usd(0.004375))
        assertEquals("$0.021", OpenRouterClient.usd(0.021))
        assertEquals("$2.50", OpenRouterClient.usd(2.5))
    }
}
