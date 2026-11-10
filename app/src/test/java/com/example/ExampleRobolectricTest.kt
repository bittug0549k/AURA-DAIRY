package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Mood
import com.example.data.model.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Diary", appName)
  }

  @Test
  fun `verify mood and weather enum mappings`() {
    assertEquals(Mood.JOY, Mood.fromName("JOY"))
    assertEquals(Mood.PEACEFUL, Mood.fromName(null))
    assertEquals(WeatherType.SUNNY, WeatherType.fromName("SUNNY"))
    assertNotNull(Mood.JOY.auraColor)
  }
}
