package com.networkradar.core.data.networking

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import com.networkradar.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class KtorInternetLatencyDataSourceTest {

    @Test
    fun `measureLatency should return success and positive duration on 200 OK`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.OK
            )
        }
        val client = HttpClient(mockEngine)
        val dataSource = KtorInternetLatencyDataSource(client)

        val result = dataSource.measureLatency("http://test.com")

        assertThat(result is Result.Success).isTrue()
        assertThat((result as Result.Success).data).isGreaterThan(0.0)
    }
}
