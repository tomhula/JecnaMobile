package me.tomasan7.jecnamobile

import me.tomasan7.jecnamobile.util.UserProfileGroupHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileGroupHelperTest
{
    @Test
    fun testParseProfileGroupExact()
    {
        assertEquals("2/2", UserProfileGroupHelper.parseProfileGroup("A2"))
        assertEquals("1/2", UserProfileGroupHelper.parseProfileGroup("A1"))
        assertEquals("2/2", UserProfileGroupHelper.parseProfileGroup("B2"))
        assertEquals("1/2", UserProfileGroupHelper.parseProfileGroup("B1"))
        assertEquals("2/2", UserProfileGroupHelper.parseProfileGroup("2/2"))
        assertEquals("1/2", UserProfileGroupHelper.parseProfileGroup("1/2"))
    }

    @Test
    fun testParseProfileGroupFromClassAndGroupsString()
    {
        assertEquals("2/2", UserProfileGroupHelper.parseProfileGroup("A2", "4.C"))
        assertEquals("1/2", UserProfileGroupHelper.parseProfileGroup("A1", "4.A"))
        assertEquals("2/2", UserProfileGroupHelper.parseProfileGroup("4.C, A2"))
        assertEquals("1/2", UserProfileGroupHelper.parseProfileGroup("1.A, A1"))
        assertEquals("2/2", UserProfileGroupHelper.parseProfileGroup("A2, B2"))
    }

    @Test
    fun testNoFalsePositives()
    {
        assertNull(UserProfileGroupHelper.parseProfileGroup(null, "4.A"))
        assertNull(UserProfileGroupHelper.parseProfileGroup(null, "1.A"))
        assertNull(UserProfileGroupHelper.parseProfileGroup(null, "2.A"))
        assertNull(UserProfileGroupHelper.parseProfileGroup(null, "3.B"))
        assertNull(UserProfileGroupHelper.parseProfileGroup(""))
        assertNull(UserProfileGroupHelper.parseProfileGroup(null, null))
    }
}
