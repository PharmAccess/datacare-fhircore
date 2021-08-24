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

package org.smartregister.fhircore.viewmodel

import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.RiskAssessment
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureMap
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.Utils

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 03-07-2021. */
@Config(shadows = [FhirApplicationShadow::class])
class QuestionnaireViewModelTest : RobolectricTest() {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var samplePatientRegisterQuestionnaire: Questionnaire
  private lateinit var questionnaireResponse: QuestionnaireResponse
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var context: Context

  @get:Rule var instantExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val qJson =
      context.assets.open("sample_patient_registration.json").bufferedReader().use { it.readText() }

    samplePatientRegisterQuestionnaire = iParser.parseResource(qJson) as Questionnaire

    val qrJson =
      context.assets.open("sample_registration_questionnaireresponse.json").bufferedReader().use {
        it.readText()
      }

    questionnaireResponse = iParser.parseResource(qrJson) as QuestionnaireResponse

    fhirEngine = mockk()

    mockkObject(FhirApplication)
    every { FhirApplication.fhirEngine(any()) } returns fhirEngine

    val savedState = SavedStateHandle()
    savedState[QUESTIONNAIRE_PATH_KEY] = "sample_patient_registration.json"
    questionnaireViewModel =
      spyk(QuestionnaireViewModel(ApplicationProvider.getApplicationContext(), savedState))
  }

  @After
  fun cleanup() {
    unmockkObject(FhirApplication)
  }

  @Test
  fun `saveBundleResources() should call saveResources()`() {
    val bundle = Bundle()
    val size = 23

    for (i in 1..size) {
      val bundleEntry = Bundle.BundleEntryComponent()
      bundleEntry.resource =
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                family = "Doe"
                given = listOf(StringType("John"))
              }
            )
        }
      bundle.addEntry(bundleEntry)
    }
    bundle.total = size

    every { questionnaireViewModel.saveResource(any()) } just runs

    // call the method under test
    questionnaireViewModel.saveBundleResources(bundle)

    verify(exactly = size) { questionnaireViewModel.saveResource(any()) }
  }

  @Test
  fun `saveBundleResources() should call saveResources and inject resourceId()`() {
    val bundle = Bundle()
    val size = 5
    val resource = slot<Resource>()
    val resourceId = "my-res-id"

    val bundleEntry = Bundle.BundleEntryComponent()
    bundleEntry.resource =
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            }
          )
      }
    bundle.addEntry(bundleEntry)
    bundle.total = size

    every { questionnaireViewModel.saveResource(any()) } just runs

    // call the method under test
    questionnaireViewModel.saveBundleResources(bundle, resourceId)

    verify(exactly = 1) { questionnaireViewModel.saveResource(capture(resource)) }

    Assert.assertEquals(resourceId, resource.captured.id)
  }

  @Test
  fun `fetchStructureMap() should call fhirEngine load and parse out the resourceId`() {
    val structureMap = StructureMap()
    val structureMapIdSlot = slot<String>()

    coEvery { fhirEngine.load(any<Class<StructureMap>>(), any()) } returns structureMap

    questionnaireViewModel.fetchStructureMap(
      ApplicationProvider.getApplicationContext(),
      "https://someorg.org/StructureMap/678934"
    )

    coVerify(exactly = 1) {
      fhirEngine.load(any<Class<StructureMap>>(), capture(structureMapIdSlot))
    }

    Assert.assertEquals("678934", structureMapIdSlot.captured)
  }

  @Test
  fun `saveExtractedResources() should call saveBundleResources and pass intent extra resourceId`() {
    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns
      samplePatientRegisterQuestionnaire

    val questionnaire = Questionnaire()
    questionnaire.extension.add(
      Extension(
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext",
        Expression().apply {
          language = "application/x-fhir-query"
          expression = "Patient"
        }
      )
    )
    val questionnaireResponse = QuestionnaireResponse()
    val intent = Intent()
    val resourceId = "0993ldsfkaljlsnldm"
    intent.putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, resourceId)

    val resourceIdSlot = slot<String>()

    every { questionnaireViewModel.saveBundleResources(any(), any()) } just runs

    runBlocking {
      questionnaireViewModel.saveExtractedResources(
        ApplicationProvider.getApplicationContext(),
        intent,
        questionnaire,
        questionnaireResponse
      )
    }
    coVerify(exactly = 1) {
      questionnaireViewModel.saveBundleResources(any(), capture(resourceIdSlot))
    }

    Assert.assertEquals(resourceId, resourceIdSlot.captured)
  }

  @Test
  fun testSaveParsedResourceShouldSaveAllResourceTypesForPatientRegister() = runBlockingTest {
    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns
      samplePatientRegisterQuestionnaire

    val patient = slot<Patient>()
    val observation = slot<Observation>()
    val riskAssessment = slot<RiskAssessment>()
    val flag = slot<Flag>()

    // comorbidities section is on 2 index
    questionnaireResponse.item[2]
      .item
      .single { it.linkId!!.contentEquals("hypertension") }
      .addAnswer()
      .value = BooleanType(true)

    runBlocking {
      questionnaireViewModel.saveParsedResource(
        questionnaireResponse,
        samplePatientRegisterQuestionnaire,
        Intent()
      )
    }

    coVerifyOrder {
      questionnaireViewModel.saveResource(capture(observation)) // comorb group obs
      questionnaireViewModel.saveResource(capture(observation))
      questionnaireViewModel.saveResource(capture(riskAssessment))
      questionnaireViewModel.saveResource(capture(flag))
      questionnaireViewModel.saveResource(capture(patient))
    }

    Assert.assertEquals("hypertension", observation.captured.code.coding[1].code)
    Assert.assertEquals("225338004", riskAssessment.captured.code.coding[0].code)
    Assert.assertEquals(
      "High Risk for COVID-19",
      riskAssessment.captured.prediction[0].outcome.text
    )
    Assert.assertEquals("870577009", flag.captured.code.coding[0].code)
    Assert.assertEquals("testeight", patient.captured.nameFirstRep.given[0].value)
  }

  @Test
  fun testSaveParsedResourceShouldSaveAllResourceTypesForFamilyRegister() = runBlockingTest {
    val questionnaire = getQuestionnaire("sample_family_registration.json")
    val questionnaireResponse =
      getQuestionnaireResponse("sample_family_registration_questionnaireresponse.json")

    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns questionnaire

    val patient = slot<Patient>()
    val observation = slot<Observation>()
    val flagPregnancy = slot<Flag>()
    val flagFamily = slot<Flag>()

    runBlocking {
      questionnaireViewModel.saveParsedResource(questionnaireResponse, questionnaire, Intent())
    }

    coVerifyOrder {
      questionnaireViewModel.saveResource(capture(observation)) // geopoint obs
      questionnaireViewModel.saveResource(capture(observation)) // income obs
      questionnaireViewModel.saveResource(capture(observation)) // living members obs
      questionnaireViewModel.saveResource(capture(flagPregnancy))
      questionnaireViewModel.saveResource(capture(flagFamily))
      questionnaireViewModel.saveResource(capture(patient))
    }

    Assert.assertEquals("77386006", flagPregnancy.captured.code.coding[0].code)
    Assert.assertEquals("Pregnant", flagPregnancy.captured.code.coding[0].display)

    Assert.assertEquals("35359004", flagFamily.captured.code.coding[0].code)
    Assert.assertEquals("Family", flagFamily.captured.code.coding[0].display)

    Assert.assertEquals("77386006", patient.captured.meta.tag[0].code)
    Assert.assertEquals("Pregnant", patient.captured.meta.tag[0].display)

    Assert.assertEquals("35359004", patient.captured.meta.tag[1].code)
    Assert.assertEquals("Family", patient.captured.meta.tag[1].display)

    Assert.assertEquals(
      "http://hl7.org/fhir/StructureDefinition/flag-detail",
      patient.captured.extension[0].url
    )
    Assert.assertEquals("Pregnant", patient.captured.extension[0].value.toString())
    Assert.assertEquals(
      "http://hl7.org/fhir/StructureDefinition/flag-detail",
      patient.captured.extension[1].url
    )
    Assert.assertEquals("Family", patient.captured.extension[1].value.toString())
  }

  @Test
  fun testSaveParsedResourceShouldSaveAllResourceTypesForFamilyMemberRegister() = runBlockingTest {
    val questionnaire = getQuestionnaire("sample_family_member_registration.json")
    val questionnaireResponse =
      getQuestionnaireResponse("sample_family_member_registration_questionnaireresponse.json")

    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns questionnaire

    val patient = slot<Patient>()
    val observation = slot<Observation>()
    val flagPregnancy = slot<Flag>()

    val intent = Intent().apply { this.putExtra(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY, "1234567") }

    runBlocking {
      questionnaireViewModel.saveParsedResource(questionnaireResponse, questionnaire, intent)
    }

    coVerifyOrder {
      questionnaireViewModel.saveResource(capture(flagPregnancy))
      questionnaireViewModel.saveResource(capture(patient))
    }

    Assert.assertEquals("77386006", flagPregnancy.captured.code.coding[0].code)
    Assert.assertEquals("Pregnant", flagPregnancy.captured.code.coding[0].display)

    Assert.assertEquals("77386006", patient.captured.meta.tag[0].code)
    Assert.assertEquals("Pregnant", patient.captured.meta.tag[0].display)

    Assert.assertEquals(
      "http://hl7.org/fhir/StructureDefinition/flag-detail",
      patient.captured.extension[0].url
    )
    Assert.assertEquals("Pregnant", patient.captured.extension[0].value.toString())

    Assert.assertEquals(Patient.LinkType.REFER, patient.captured.link[0].type)
    Assert.assertEquals("Patient/1234567", patient.captured.link[0].other.reference)
  }

  @Test
  fun testVerifyQuestionnaireSubjectType() {
    coEvery { fhirEngine.load(Questionnaire::class.java, any()) } returns
      samplePatientRegisterQuestionnaire

    val questionnaire = questionnaireViewModel.questionnaire

    Assert.assertNotNull(questionnaire)
    Assert.assertEquals("Patient", questionnaire.subjectType[0].code)
  }

  @Test
  fun `getStructureMapProvider() should return valid provider`() {
    Assert.assertNull(questionnaireViewModel.structureMapProvider)

    Assert.assertNotNull(
      questionnaireViewModel.getStructureMapProvider(ApplicationProvider.getApplicationContext())
    )
  }

  @Test
  fun `structureMapProvider should call fetchStructureMap()`() {
    val resourceUrl = "https://fhir.org/StructureMap/89"
    val structureMapProvider =
      questionnaireViewModel.getStructureMapProvider(ApplicationProvider.getApplicationContext())

    every { questionnaireViewModel.fetchStructureMap(any(), any()) } returns StructureMap()

    runBlocking { structureMapProvider.invoke(resourceUrl) }

    verify { questionnaireViewModel.fetchStructureMap(any(), resourceUrl) }
  }

  private fun getQuestionnaire(id: String): Questionnaire {
    val qJson = context.assets.open(id).bufferedReader().use { it.readText() }

    return Utils.parser.parseResource(qJson) as Questionnaire
  }

  private fun getQuestionnaireResponse(id: String): QuestionnaireResponse {
    val qrJson = context.assets.open(id).bufferedReader().use { it.readText() }

    return Utils.parser.parseResource(qrJson) as QuestionnaireResponse
  }
}