package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testTtsLanguageModel() {
    val bnLang = TtsLanguage(
      displayName = "বাংলা (বাংলাদেশ)",
      locale = Locale("bn", "BD"),
      sampleText = "সুপ্রভাত!"
    )
    assertEquals("বাংলা (বাংলাদেশ)", bnLang.displayName)
    assertEquals("bn", bnLang.locale.language)
    assertEquals("BD", bnLang.locale.country)
    assertTrue(bnLang.sampleText.isNotEmpty())
  }
}
