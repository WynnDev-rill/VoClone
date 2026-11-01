package id.wynn.voclone

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.work.workDataOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Presentation fixture only: no inference success is simulated or claimed. */
class ProgressUiTest {
    @get:Rule val rule=createComposeRule()
    @Test fun reportsRealStepFractionAndPartsAndCancel() {
        var cancelled=false
        rule.setContent { VoCloneTheme("light") {
            TaskProgress(workDataOf("kind" to "inference","phase" to "Menyintesis suara…","stage" to 3,
                "done" to 8L,"total" to 32L,"part" to 2,"parts" to 5)) { cancelled=true }
        } }
        rule.onNodeWithText("25% tahap sintesis • iterasi selesai 8/32").assertIsDisplayed()
        rule.onNodeWithText("Bagian teks 2/5").assertIsDisplayed()
        rule.onNodeWithText("Batalkan").performClick()
        rule.runOnIdle { assertTrue(cancelled) }
    }
    @Test fun modelLoadingDoesNotInventPercentage() {
        rule.setContent { VoCloneTheme("light") { TaskProgress(workDataOf("kind" to "inference","stage" to 1,"phase" to "Memuat model…")) {} } }
        rule.onNodeWithText("Memuat model…").assertIsDisplayed()
        rule.onAllNodes(hasText("%",substring=true)).assertCountEquals(0)
    }
}
