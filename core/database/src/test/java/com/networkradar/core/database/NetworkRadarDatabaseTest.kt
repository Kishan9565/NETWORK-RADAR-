package com.networkradar.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.networkradar.core.database.dao.NetworkMeasurementPointDao
import com.networkradar.core.database.dao.ScanSessionDao
import com.networkradar.core.database.entity.NetworkMeasurementPointEntity
import com.networkradar.core.database.entity.ScanSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class NetworkRadarDatabaseTest {

    private lateinit var db: NetworkRadarDatabase
    private lateinit var sessionDao: ScanSessionDao
    private lateinit var pointDao: NetworkMeasurementPointDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NetworkRadarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = db.scanSessionDao()
        pointDao = db.measurementPointDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `insert and get session`() = runTest {
        val session = ScanSessionEntity("session-1", false, "Scan 1", 1000L, null, 0)
        sessionDao.insertSession(session)
        
        val retrieved = sessionDao.getSessionById("session-1")
        assertThat(retrieved).isEqualTo(session)
    }

    @Test
    fun `insert point and increment count`() = runTest {
        val session = ScanSessionEntity("session-1", false, "Scan 1", 1000L, null, 0)
        sessionDao.insertSession(session)
        
        val point = NetworkMeasurementPointEntity(
            sessionId = "session-1",
            timestamp = 1100L,
            latitude = 50.0,
            longitude = 10.0,
            locationAccuracy = 5.0f,
            locationTimestamp = 1100L,
            indoorX = null,
            indoorY = null,
            indoorTimestamp = null,
            wifi = null,
            cellular = null,
            internet = null
        )
        pointDao.insertMeasurementPoint(point)
        sessionDao.incrementMeasurementCount("session-1")
        
        val updatedSession = sessionDao.getSessionById("session-1")
        assertThat(updatedSession?.measurementCount).isEqualTo(1)
        
        val points = pointDao.getMeasurementsForSession("session-1").first()
        assertThat(points.size).isEqualTo(1)
        assertThat(points[0].sessionId).isEqualTo("session-1")
    }

    @Test
    fun `cascade delete session deletes points`() = runTest {
        val session = ScanSessionEntity("session-1", false, "Scan 1", 1000L, null, 0)
        sessionDao.insertSession(session)
        
        val point = NetworkMeasurementPointEntity(
            sessionId = "session-1",
            timestamp = 1100L,
            latitude = 50.0,
            longitude = 10.0,
            locationAccuracy = 5.0f,
            locationTimestamp = 1100L,
            indoorX = null,
            indoorY = null,
            indoorTimestamp = null,
            wifi = null,
            cellular = null,
            internet = null
        )
        pointDao.insertMeasurementPoint(point)
        
        val pointsBefore = pointDao.getMeasurementsForSession("session-1").first()
        assertThat(pointsBefore.size).isEqualTo(1)

        sessionDao.deleteSessionById("session-1")
        val pointsAfter = pointDao.getMeasurementsForSession("session-1").first()
        assertThat(pointsAfter.size).isEqualTo(0)
    }
}
