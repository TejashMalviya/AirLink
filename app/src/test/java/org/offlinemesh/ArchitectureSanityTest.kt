package org.offlinemesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ArchitectureSanityTest {

    @Test
    fun testShellSanity() {
        val appName = "AirLink"
        assertNotNull(appName)
        assertEquals("AirLink", appName)
    }
}
