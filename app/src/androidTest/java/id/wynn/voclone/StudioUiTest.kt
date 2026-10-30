package id.wynn.voclone

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File
import android.graphics.Bitmap

class StudioUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun opensDirectlyToCloneAndSettingsWithoutBottomNavigation() {
        rule.onNodeWithText("Clone Your Voice").assertIsDisplayed()
        rule.onNodeWithText("Audio Reference").assertIsDisplayed()
        screenshot("clone")
        rule.onNodeWithContentDescription("Pengaturan").performClick()
        rule.onNodeWithText("Model AI offline").assertIsDisplayed()
        screenshot("settings")
        rule.onNodeWithContentDescription("Kembali").performClick()
        rule.onNodeWithText("Clone Your Voice").assertIsDisplayed()
    }
    private fun screenshot(name: String) {
        rule.waitForIdle()
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val out=File(ctx.getExternalFilesDir(null),"qa/screenshots/$name.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
