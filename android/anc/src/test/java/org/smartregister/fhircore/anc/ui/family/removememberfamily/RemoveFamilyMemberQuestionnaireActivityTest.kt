/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.anc.ui.family.removememberfamily

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.test.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.details.form.FormConfig.REMOVE_FAMILY_FORM
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.family.removefamilymember.RemoveFamilyMemberQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.family.removefamilymember.RemoveFamilyMemberQuestionnaireViewModel
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetDataModel
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetHolder
import org.smartregister.fhircore.anc.util.othersEligibleForHead
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class RemoveFamilyMemberQuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @BindValue
  val questionnaireViewModel: QuestionnaireViewModel =
    spyk(QuestionnaireViewModel(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk()))

  @BindValue
  val viewModel: RemoveFamilyMemberQuestionnaireViewModel =
    spyk(
      RemoveFamilyMemberQuestionnaireViewModel(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk()
      )
    )

  @BindValue
  val configurationRegistry: ConfigurationRegistry =
    spyk(ConfigurationRegistry(mockk(), mockk(), mockk()))

  private lateinit var activity: RemoveFamilyMemberQuestionnaireActivity

  private lateinit var familyDetailsActivity: FamilyDetailsActivity
  private var familyDetailRepository: FamilyDetailRepository = mockk(relaxed = true)
  @Before
  fun setUp() {
    hiltRule.inject()
    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs

    familyDetailsActivity =
      Robolectric.buildActivity(FamilyDetailsActivity::class.java).create().resume().get()
    familyDetailsActivity.familyName = "Test Family"
  }

  @Test
  fun testActivityShouldNotNull() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    Assert.assertNotNull(activity)
  }

  @Test
  fun testOnBackPressedShouldCallConfirmationDialogue() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    activity.onBackPressed()

    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    Assert.assertNotNull(alertDialog)
    Assert.assertEquals("", alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).text)
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_back_pressed_button_title),
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text
    )
  }

  override fun getActivity(): Activity {
    return activity
  }

  @Test
  fun testBottomSheetList() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    val bottomSheetHolder = BottomSheetHolder("", "", "", listOf(BottomSheetDataModel("", "", "")))
    Assert.assertNotNull(bottomSheetHolder)
  }

  @Test
  fun testHandleQuestionResponse() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    activity.handleQuestionnaireResponse(QuestionnaireResponse())
  }

  @Test
  fun testOnReceivingIntentFromRemoveFamilyMember() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    val intent = Intent(activity, FamilyDetailsActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, "asui32unfd")
    InstrumentationRegistry.getTargetContext().startActivity(intent)
  }

  @Test
  fun testShouldGoToPatientDetailScreen() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    activity.switchToPatientScreen()
    val expectedIntent = Intent(activity, FamilyDetailsActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>())
        .nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnShowFamilyHeadDialog() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    viewModel.shouldOpenHeadDialog.value = true
    coEvery { viewModel.familyMembers.othersEligibleForHead() } returns arrayListOf()
  }

  @Test
  fun testOnOpenBottomSheetDialog() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    coEvery { viewModel.familyMembers.othersEligibleForHead() } returns arrayListOf()
    activity.openBottomSheetDialog(arrayListOf())
  }

  @Test
  fun testSetUI() {
    buildActivityFor(REMOVE_FAMILY_FORM, false)
    activity.setupUI()
    Assert.assertEquals(
      getString(R.string.questionnaire_remove_family_member_btn_save_client_info),
      activity.saveBtn.text.toString()
    )
  }

  private fun buildActivityFor(form: String, editForm: Boolean, headId: String? = null) {
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply {
        name = form
        title = form
        if (form == FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM)
          addItem().linkId = FamilyQuestionnaireActivity.HEAD_RECORD_ID_KEY
      }
    every { configurationRegistry.appId } returns "anc"

    val intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, form)
        putExtra(
          QuestionnaireActivity.QUESTIONNAIRE_ARG_TYPE,
          if (editForm) QuestionnaireType.EDIT.name else QuestionnaireType.DEFAULT.name
        )
        putExtra(FamilyQuestionnaireActivity.QUESTIONNAIRE_RELATED_TO_KEY, headId)
      }

    activity =
      Robolectric.buildActivity(RemoveFamilyMemberQuestionnaireActivity::class.java, intent)
        .create()
        .get()
  }
}
