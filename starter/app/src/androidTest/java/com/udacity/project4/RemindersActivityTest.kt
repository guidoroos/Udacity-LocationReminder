package com.udacity.project4

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.ToastMatcher.Companion.onToast
import com.udacity.project4.utils.EspressoIdlingResource.wrapEspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }



    @Test
    fun testCreateNewReminderIsShownOnReminderList() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        //navigate to saveReminder fragment by clicking fab
        onView(withId(R.id.addReminderFAB)).perform(click())

        //add title and description
        onView(withId(R.id.reminderTitle))
            .perform(typeText("test title"))

        onView(withId(R.id.reminderDescription))
            .perform(typeText("test description"))

        closeSoftKeyboard()

        //navigate to select location fragment and back to get rid of snackbar
        onView(withId(R.id.selectLocation)).perform(click())
        pressBack()

        //navigate to select location fragment
        onView(withId(R.id.selectLocation)).perform(click())

        //use defaults for select location and navigate back to savereminder fragment
        onView(withId(R.id.fab_save_location)).perform(click())

        //save reminder and navigate to reminderlist
            onView(withId(R.id.saveReminder)).perform(click())

        //check toast shown NOT ABLE to get this working
        //val toastText = getApplicationContext<MyApp>().getString(R.string.geofences_added,"test title")
        //onToast(toastText).check(matches(isDisplayed()))

        //test added reminder is in list on screen
        onView(withId(R.id.reminderssRecyclerView))
            .perform(
                // scrollTo will fail the test if no item matches.
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(ViewMatchers.withText("Cheese Market Alkmaar"))
                ))

    }


    @Test
    fun testValidationSnackBarTitle () {
        ActivityScenario.launch(RemindersActivity::class.java)

        //navigate to saveReminder fragment by clicking fab
        onView(withId(R.id.addReminderFAB)).perform(click())

        //check if snackbar shown when title not there
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))
    }

    @Test
    fun testValidationSnackBarLocation () {
        ActivityScenario.launch(RemindersActivity::class.java)

        //navigate to saveReminder fragment by clicking fab
        onView(withId(R.id.addReminderFAB)).perform(click())

        //add title and description
        onView(withId(R.id.reminderTitle))
            .perform(typeText("test title"))

        onView(withId(R.id.reminderDescription))
            .perform(typeText("test description"))

        closeSoftKeyboard()

        //check if snackbar shown when location not there
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

    }

}
