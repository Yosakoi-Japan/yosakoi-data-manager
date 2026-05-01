package jp.yosakoi.sync.contract

import kotlin.test.Test
import kotlin.test.assertContains
import java.nio.file.Files
import java.nio.file.Path

class WorkflowFileTest {
    @Test
    fun `workflow contains required triggers and steps`() {
        val workflow = Files.readString(Path.of(".github/workflows/sync-events.yml"))

        assertContains(workflow, "workflow_dispatch:")
        assertContains(workflow, "schedule:")
        assertContains(workflow, "./gradlew run --args=")
        assertContains(workflow, "repository: Yosakoi-Japan/yosakoiPortal")
        assertContains(workflow, "frontend/app/src/assets/data/yosakoi_event.csv")
        assertContains(workflow, "YOSAKOI_PORTAL_PAT")
    }
}
