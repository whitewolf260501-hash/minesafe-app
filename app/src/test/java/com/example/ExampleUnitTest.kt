package com.example

import com.example.model.GatewayData
import com.example.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGatewayData_defaults() {
    val gw = GatewayData()
    assertEquals("gateway", gw.deviceId)
    assertEquals("192.168.1.100", gw.ip)
    assertEquals(8080, gw.puerto)
    assertEquals("24:6F:28:B4:7C:12", gw.mac)
    assertEquals(4, gw.nodosConectados)
  }

  @Test
  fun testUserProfile_adminPermissions() {
    val superAdmin = UserProfile(tipoUsuario = "superAdmin", isSuperUser = true)
    assertTrue(superAdmin.canAccessAdminPanel)
    assertEquals("superAdmin", superAdmin.rolEfectivo)

    val admin = UserProfile(tipoUsuario = "admin", isAdmin = true)
    assertTrue(admin.canAccessAdminPanel)
    assertEquals("admin", admin.rolEfectivo)

    val operator = UserProfile(tipoUsuario = "usuario", isAdmin = false, isSuperUser = false)
    assertFalse(operator.canAccessAdminPanel)
  }
}

