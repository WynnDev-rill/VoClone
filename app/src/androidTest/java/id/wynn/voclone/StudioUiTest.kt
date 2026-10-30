package id.wynn.voclone

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File
import android.graphics.Bitmap
import androidx.compose.ui.unit.dp

class StudioUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun opensDirectlyToCloneAndSettingsWithoutBottomNavigation() {
        rule.onNodeWithText("Clone Your Voice").assertIsDisplayed()
        rule.onNodeWithText("Audio Reference").assertIsDisplayed()
        screenshot("clone")
        rule.onNodeWithContentDescription("Pengaturan").performClick()
        rule.onNodeWithText("Model AI offline").assertIsDisplayed()
        val consent="Saya membaca dan menyetujui ketentuan model."
        if(rule.onAllNodesWithText(consent).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText(consent).assertHeightIsAtLeast(48.dp)
        }
        screenshot("settings")
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Gelap"))
        rule.onNodeWithText("Gelap").performClick()
        screenshot("settings-dark")
        rule.onNodeWithContentDescription("Kembali").performClick()
        rule.onNodeWithText("Clone Your Voice").assertIsDisplayed()
        screenshot("clone-dark")
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Generate Audio"))
        rule.onNodeWithText("Generate Audio").assertIsDisplayed()
        screenshot("generate-dark")
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        rule.onNodeWithContentDescription("Pengaturan").performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Ikuti sistem"))
        rule.onNodeWithText("Ikuti sistem").performClick()
        rule.onNodeWithContentDescription("Kembali").performClick()
    }
    private fun screenshot(name: String) {
        rule.waitForIdle()
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val suffix=InstrumentationRegistry.getArguments().getString("screenshotSuffix").orEmpty().take(40).replace(Regex("[^a-zA-Z0-9_-]"),"_")
        val filename=if(suffix.isBlank())name else "$name-$suffix"
        val out=File(ctx.getExternalFilesDir(null),"qa/screenshots/$filename.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
