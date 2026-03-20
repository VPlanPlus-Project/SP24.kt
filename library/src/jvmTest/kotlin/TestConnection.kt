import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import plus.vplan.lib.sp24.source.Authentication
import plus.vplan.lib.sp24.source.TestConnectionResult

class TestConnection {
    @Test
    fun nonExistentConnection() = runBlocking {
        val result = stundenplan24Client.testConnection(Authentication("00000000", "invalidUser", "invalidPass"))
        assertNotNull(result)
        assertEquals(TestConnectionResult.NotFound, result)
    }

    @Test
    fun invalidCredentials() = runBlocking {
        val response = stundenplan24Client.testConnection(Authentication("10063764", "invalidUser", "invalidPass"))
        assertNotNull(response)
        assertEquals(TestConnectionResult.Unauthorized, response)
    }

    @Test
    fun validConnection() = runBlocking {
        val response = stundenplan24Client.testConnection(Authentication("10000000", "schueler", "123123"))
        assertNotNull(response)
        assertEquals(TestConnectionResult.Success, response)
    }
}