package com.networkradar.core.data.networking

import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

class HttpClientExtTest {

    @Serializable
    data class TestDto(val id: Int)

    private fun createClient(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    @Test
    fun `200 response should return Success`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"id": 1}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = createClient(mockEngine)

        val result = client.get<TestDto>("http://test.com")

        assertThat(result is Result.Success).isEqualTo(true)
        assertThat((result as Result.Success).data.id).isEqualTo(1)
    }

    @Test
    fun `404 response should return NOT_FOUND`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.NotFound
            )
        }
        val client = createClient(mockEngine)
        
        val result = client.get<TestDto>("http://test.com")

        assertThat(result is Result.Error).isEqualTo(true)
        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.NOT_FOUND)
    }

    @Test
    fun `401 response should return UNAUTHORIZED`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.Unauthorized
            )
        }
        val client = createClient(mockEngine)
        
        val result = client.get<TestDto>("http://test.com")

        assertThat(result is Result.Error).isEqualTo(true)
        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.UNAUTHORIZED)
    }

    @Test
    fun `429 response should return TOO_MANY_REQUESTS`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.TooManyRequests
            )
        }
        val client = createClient(mockEngine)
        
        val result = client.get<TestDto>("http://test.com")

        assertThat(result is Result.Error).isEqualTo(true)
        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.TOO_MANY_REQUESTS)
    }

    @Test
    fun `500 response should return SERVER_ERROR`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError
            )
        }
        val client = createClient(mockEngine)

        val result = client.get<TestDto>("http://test.com")

        assertThat(result is Result.Error).isEqualTo(true)
        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.SERVER_ERROR)
    }
}
