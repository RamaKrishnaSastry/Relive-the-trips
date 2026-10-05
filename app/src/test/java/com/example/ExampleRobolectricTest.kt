package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthRepository
import com.example.auth.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Temple Map", appName)
  }

  @Test
  fun `auth repository demo sign-in and session persistence test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = AuthRepository(context)
    
    // Initially unauthenticated
    assertTrue(repo.authState.value is AuthState.Unauthenticated)

    // Sign in as demo user
    val user = repo.signInAsDemo(email = "pilgrim@templemap.app", displayName = "Temple Pilgrim")
    assertEquals("pilgrim@templemap.app", user.email)
    assertEquals("Temple Pilgrim", user.displayName)
    assertTrue(repo.authState.value is AuthState.Authenticated)

    // Create a new repo instance with same context (simulating app relaunch)
    val restoredRepo = AuthRepository(context)
    val restoredState = restoredRepo.authState.value
    assertTrue(restoredState is AuthState.Authenticated)
    val restoredUser = (restoredState as AuthState.Authenticated).user
    assertEquals("pilgrim@templemap.app", restoredUser.email)
    assertEquals("Temple Pilgrim", restoredUser.displayName)

    // Sign out
    restoredRepo.signOut()
    assertTrue(restoredRepo.authState.value is AuthState.Unauthenticated)
  }
}
