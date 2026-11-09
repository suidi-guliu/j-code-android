package dev.blamspot.jcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Uses only APIs present in the original v0.5 APK. No DB edits, fake runtime readiness or installs. */
@RunWith(AndroidJUnit4::class)
class BaselineDataTest {
    @Test fun createRegisteredProject() = checkProject(create=true)
    @Test fun verifyRegisteredProjectSurvives() = checkProject(create=false)
    private fun checkProject(create: Boolean) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: MainViewModel
            scenario.onActivity { activity ->
                model=ViewModelProvider(activity)[MainViewModel::class.java]
                if(create) model.createNewItem(MainViewModel.NewItemRequest("PreservedProject",false,"empty",emptyMap()))
            }
            val deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(30)
            while(model.currentWorkspace.value?.projects?.none { it.name=="PreservedProject" }!=false && System.nanoTime()<deadline) Thread.sleep(100)
            assertTrue("Original registered Project must exist without creating it during verification",model.currentWorkspace.value?.projects?.any { it.name=="PreservedProject" }==true)
        }
    }
}
