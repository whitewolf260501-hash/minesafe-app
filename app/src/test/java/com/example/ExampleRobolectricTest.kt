package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AlertDeviceReading
import com.example.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches MineSafe`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MineSafe", appName)
    }

    @Test
    fun `test exact role calculation rules`() {
        val super1 = UserProfile(isSuperUser = true, isAdmin = false, tipoUsuario = "usuario")
        assertEquals("superAdmin", super1.rolEfectivo)

        val super2 = UserProfile(isSuperUser = false, isAdmin = false, tipoUsuario = "superAdmin")
        assertEquals("superAdmin", super2.rolEfectivo)

        val admin1 = UserProfile(isSuperUser = false, isAdmin = true, tipoUsuario = "usuario")
        assertEquals("admin", admin1.rolEfectivo)

        val admin2 = UserProfile(isSuperUser = false, isAdmin = false, tipoUsuario = "admin")
        assertEquals("admin", admin2.rolEfectivo)

        val standard = UserProfile(isSuperUser = false, isAdmin = false, tipoUsuario = "usuario")
        assertEquals("usuario", standard.rolEfectivo)
    }

    @Test
    fun `test safety alert rules evaluation`() {
        // Safe conditions
        val safeReading = AlertDeviceReading(
            co = 20.0,
            co2 = 600.0,
            pm10 = 50.0,
            pm25 = 20.0,
            humedad = 60.0
        )
        assertTrue(safeReading.isAllSafe)
        assertEquals(0, safeReading.activeAlerts.size)

        // Danger conditions exceeding thresholds
        val dangerReading = AlertDeviceReading(
            co = 55.0,        // > 50
            co2 = 1200.0,     // > 1000
            pm10 = 120.0,     // > 100
            pm25 = 55.0,      // > 50
            humedad = 85.0    // > 80
        )
        assertFalse(dangerReading.isAllSafe)
        assertEquals(5, dangerReading.activeAlerts.size)
    }
}
