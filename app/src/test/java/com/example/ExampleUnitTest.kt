package com.example

import com.example.data.model.EmailDocIdHelper
import com.example.data.model.IndianPhoneValidator
import com.example.data.model.LoginRole
import com.example.data.model.OrderStatus
import com.example.data.model.RestaurantSettings
import com.example.data.model.UserProfile
import com.example.notifications.OrderAlertManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun indianPhoneValidator_acceptsValidTenDigitNumbersWithAndWithoutCountryCode() {
    assertTrue(IndianPhoneValidator.isValidTenDigitIndianMobile("9876543210"))
    assertTrue(IndianPhoneValidator.isValidTenDigitIndianMobile("6000012345"))
    assertEquals("+919876543210", IndianPhoneValidator.toE164("9876543210"))
    assertEquals("+919876543210", IndianPhoneValidator.toE164("+91 98765 43210"))
    assertEquals("+918123456789", IndianPhoneValidator.toE164("08123456789"))
  }

  @Test
  fun indianPhoneValidator_rejectsInvalidNumbers() {
    assertFalse(IndianPhoneValidator.isValidTenDigitIndianMobile("5876543210"))
    assertFalse(IndianPhoneValidator.isValidTenDigitIndianMobile("987654321"))
    assertNull(IndianPhoneValidator.toE164("1234567890"))
    assertNull(IndianPhoneValidator.toE164(""))
  }

  @Test
  fun emailDocIdHelper_normalizesMatchingFirestoreRuleLogic() {
    assertTrue(EmailDocIdHelper.isValidEmail("junaid545ansrai@gmail.com"))
    assertTrue(EmailDocIdHelper.isValidEmail(" Manager.TFC@Example.COM "))
    assertFalse(EmailDocIdHelper.isValidEmail("invalid-email"))
    assertFalse(EmailDocIdHelper.isValidEmail("user @gmail.com"))

    assertEquals(
      "junaid545ansrai_gmail_com",
      EmailDocIdHelper.fromEmail("Junaid545ansrai@Gmail.com")
    )
    assertEquals(
      "manager_one_tfc_co_in",
      EmailDocIdHelper.fromEmail("manager.one@tfc.co.in")
    )
  }

  @Test
  fun userProfile_isProfileCompleted_requiresNameAndValidIndianMobile() {
    val newlySignedIn = UserProfile(
      uid = "uid-1",
      name = "Aarav Sharma",
      email = "aarav@gmail.com",
      phoneNumber = "",
      role = LoginRole.CUSTOMER.raw
    )
    assertFalse(newlySignedIn.isProfileCompleted)

    val completedProfile = newlySignedIn.copy(phoneNumber = "+919876543210")
    assertTrue(completedProfile.isProfileCompleted)

    val legacyCompleted = UserProfile(
      id = "uid-legacy",
      displayName = "Legacy User",
      phone = "+919123456789",
      photoUrl = "https://example.com/avatar.png"
    )
    assertTrue(legacyCompleted.isProfileCompleted)
    assertEquals("uid-legacy", legacyCompleted.effectiveUid)
    assertEquals("Legacy User", legacyCompleted.effectiveName)
    assertEquals("+919123456789", legacyCompleted.effectivePhone)
    assertEquals("https://example.com/avatar.png", legacyCompleted.effectiveProfileImage)
  }

  @Test
  fun loginRole_fromRaw_defaultsSafelyToCustomer() {
    assertEquals(LoginRole.OWNER, LoginRole.fromRaw("owner"))
    assertEquals(LoginRole.MANAGER, LoginRole.fromRaw("manager"))
    assertEquals(LoginRole.CUSTOMER, LoginRole.fromRaw("customer"))
    assertEquals(LoginRole.CUSTOMER, LoginRole.fromRaw("unknown_role"))
    assertEquals(LoginRole.CUSTOMER, LoginRole.fromRaw(null))
  }

  @Test
  fun orderAlertManager_andOrderStatus_behaveDeterministically() {
    val idA = OrderAlertManager.notificationIdForOrder("order_101")
    val idB = OrderAlertManager.notificationIdForOrder("order_102")
    assertEquals(idA, OrderAlertManager.notificationIdForOrder("order_101"))
    assertNotEquals(idA, idB)

    assertFalse(OrderStatus.ORDER_PLACED.isTerminal)
    assertFalse(OrderStatus.ACCEPTED.isTerminal)
    assertTrue(OrderStatus.DELIVERED.isTerminal)
    assertTrue(OrderStatus.CANCELLED.isTerminal)
    assertEquals(0.0, RestaurantSettings().minimumOrderAmount, 0.001)
  }
}

