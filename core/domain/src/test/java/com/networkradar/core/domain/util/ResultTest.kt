package com.networkradar.core.domain.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ResultTest {

    @Test
    fun `map should transform success data`() {
        val result: Result<Int, DataError.Network> = Result.Success(5)
        val mapped = result.map { it * 2 }

        assertThat((mapped as Result.Success).data).isEqualTo(10)
    }

    @Test
    fun `map should preserve error`() {
        val result: Result<Int, DataError.Network> = Result.Error(DataError.Network.BAD_REQUEST)
        val mapped = result.map { it * 2 }

        assertThat((mapped as Result.Error).error).isEqualTo(DataError.Network.BAD_REQUEST)
    }

    @Test
    fun `onSuccess should be called for Success`() {
        var called = false
        val result: Result<Int, DataError.Network> = Result.Success(5)
        
        result.onSuccess { called = true }
        
        assertThat(called).isEqualTo(true)
    }

    @Test
    fun `onFailure should be called for Error`() {
        var called = false
        val result: Result<Int, DataError.Network> = Result.Error(DataError.Network.BAD_REQUEST)
        
        result.onFailure { called = true }
        
        assertThat(called).isEqualTo(true)
    }
}
