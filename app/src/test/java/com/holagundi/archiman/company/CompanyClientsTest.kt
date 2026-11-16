package com.holagundi.archiman.company

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CompanyClientsTest {
    private fun row(id: String = "11111111-1111-4111-8111-111111111111") = JSONObject()
        .put("id",id).put("name","Demo client").put("kind","individual").put("version",1)
        .apply { listOf("contactPerson","email","phone","address","correspondenceAddress","preferredCommunication","notes").forEach { put(it,"") } }
    private fun page(vararg rows:JSONObject) = JSONObject().put("clients",JSONArray(rows.toList())).put("nextCursor",JSONObject.NULL)
    private fun rejected(value:JSONObject,after:String?=null) {
        try { readCompanyClients(value,after); fail("Malformed contact page accepted") }
        catch(error:CompanyApiException) { assertEquals("INVALID_RESPONSE",error.errorCode) }
    }
    @Test fun `reads bounded contact records without requiring financial identifiers`() {
        val parsed=readCompanyClients(page(row().put("email","demo@example.test")),null)
        assertEquals("demo@example.test",parsed.clients.single().email)
        assertNull(parsed.nextCursor)
        assertTrue(readCompanyClients(page(),null).clients.isEmpty())
    }
    @Test fun `rejects malformed records duplicates and oversized pages`() {
        rejected(page(row("bad")))
        rejected(page(row().put("version",0)))
        rejected(page(row().put("version",2147483648L)))
        rejected(page(row().put("kind","unknown")))
        rejected(page(row().put("name"," ")))
        rejected(page(row().put("notes","x".repeat(4001))))
        rejected(page(row(),row()))
        rejected(page(*(1..26).map { row(java.util.UUID.randomUUID().toString()) }.toTypedArray()))
    }
    @Test fun `cursor must identify the last contact on a full page and advance`() {
        val rows=(1..25).map { row(java.util.UUID.randomUUID().toString()) }.toTypedArray()
        val last=rows.last().getString("id")
        val value=page(*rows).put("nextCursor",last)
        assertEquals(last,readCompanyClients(value,null).nextCursor)
        rejected(value,last)
        rejected(page(row()).put("nextCursor",last))
        rejected(value.put("nextCursor",rows.first().getString("id")))
    }
}
