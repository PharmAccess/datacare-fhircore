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

package org.smartregister.fhircore.engine.data.local

import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.patient.dao.register.family.FamilyRegisterDao
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathHostServices

@HiltAndroidTest
class FamilyDaoRegisterTest : RobolectricTest() {
  @get:Rule val hiltRule = HiltAndroidRule(this)

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry(mockk())

  val fhirEngine: FhirEngine = mockk()

  val hapiWorkerContext =
    FhirContext.forR4Cached().let { HapiWorkerContext(it, it.validationSupport) }

  val fhirPathEngine: FHIRPathEngine =
    FHIRPathEngine(hapiWorkerContext).apply { hostServices = FhirPathHostServices() }

  @BindValue
  val defaultRepository: DefaultRepository =
    spyk(
      DefaultRepository(
        fhirEngine = fhirEngine,
        fhirPathEngine = fhirPathEngine,
        dispatcherProvider = DefaultDispatcherProvider()
      )
    )

  @Inject lateinit var familyRegisterDao: FamilyRegisterDao

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun loadRegisterData() = runTest {
    coEvery { defaultRepository.searchResource(ResourceType.Patient, any(), any(), any()) } returns
      listOf(buildPatient(1), buildPatient(2))

    coEvery { defaultRepository.searchResource(ResourceType.CarePlan, any(), any(), any()) } returns
      listOf()

    coEvery {
      defaultRepository.searchResource(ResourceType.Condition, any(), any(), any())
    } returns listOf()

    familyRegisterDao.loadRegisterData(0, true, null).apply { println(this.toString()) }
  }

  private fun buildPatient(serial: Int): Patient {
    return Patient().apply {
      id = serial.toString()
      addName().apply {
        addGiven("PGiven$serial")
        family = "PFamily$serial"
      }
    }
  }
}
