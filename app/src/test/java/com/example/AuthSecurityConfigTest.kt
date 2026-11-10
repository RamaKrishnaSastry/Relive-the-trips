package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.auth.FirebaseConfigLoader
import com.example.auth.SigningCertificateValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthSecurityConfigTest {

    @Test
    fun testWebClientIdLoadedFromFirebaseConfig() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = FirebaseConfigLoader.loadConfig(context)

        assertNotNull("Config should not be null", config)
        assertEquals("gen-lang-client-0732240611", config.projectId)
        assertEquals("445159597263-up7cnu2io7jpj27jhgvotl2pg8emkrjg.apps.googleusercontent.com", config.oAuthClientId)
        assertFalse("Web Client ID should not contain placeholder", config.oAuthClientId.contains("placeholder"))
    }

    @Test
    fun testSigningValidatorDiagnosticsForDeveloperError() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val devErrorException = IllegalStateException("10: DEVELOPER_ERROR: UNREGISTERED_ON_API_CONSOLE")
        val diagnosis = SigningCertificateValidator.diagnoseSignInException(context, devErrorException)

        assertTrue("Diagnosis should mention DEVELOPER_ERROR", diagnosis.contains("DEVELOPER_ERROR"))
        assertTrue("Diagnosis should mention SHA-1", diagnosis.contains("SHA-1"))
        assertTrue("Diagnosis should mention package name", diagnosis.contains(context.packageName))
    }

    @Test
    fun testExpectedSha1ConstantIsAccurate() {
        val expectedSha1 = SigningCertificateValidator.EXPECTED_DEBUG_SHA1
        assertEquals("83:D7:48:9A:3C:E4:0B:B5:33:AA:0F:EB:81:5C:50:38:AD:CC:7E:52", expectedSha1)
    }
}
