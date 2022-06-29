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

package org.smartregister.fhircore.quest.app.fakes

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import java.io.File
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.quest.robolectric.RobolectricTest.Companion.readFile

object Faker {

  fun buildPatient(
    id: String = "sampleId",
    family: String = "Mandela",
    given: String = "Nelson",
    age: Int = 78,
    gender: Enumerations.AdministrativeGender = Enumerations.AdministrativeGender.MALE
  ): Patient {
    return Patient().apply {
      this.id = id
      this.identifierFirstRep.value = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.gender = gender
      this.birthDate = DateType(Date()).apply { add(Calendar.YEAR, -age) }.dateTimeValue().value

      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
    }
  }

  val systemPath =
    (System.getProperty("user.dir") +
      File.separator +
      "src" +
      File.separator +
      "main" +
      File.separator +
      "assets" +
      File.separator)

  fun loadTestConfigurationRegistryData(
    appId: String,
    defaultRepository: DefaultRepository,
    configurationRegistry: ConfigurationRegistry
  ) {
    val composition =
      getBasePath(appId, "composition").readFile(systemPath).decodeResourceFromString() as
        Composition
    coEvery { defaultRepository.searchCompositionByIdentifier(any()) } returns composition

    coEvery { defaultRepository.getBinary(any()) } answers
      {
        val sectionComponent =
          composition.section.find {
            this.args.first().toString() == it.focus.reference.substringAfter("Binary/")
          }
        val classification = sectionComponent!!.focus.identifier.value
        Binary().apply {
          content = getBasePath(appId, classification).readFile(systemPath).toByteArray()
        }
      }

    runBlocking { configurationRegistry.loadConfigurations(appId) {} }
  }

  private fun getBasePath(appId: String, classification: String): String {
    return "/configs/$appId/config_$classification.json"
  }

  fun buildTestConfigurationRegistry(
    appId: String,
    defaultRepository: DefaultRepository
  ): ConfigurationRegistry {
    val configurationRegistry =
      spyk(ConfigurationRegistry(mockk(), mockk(), mockk(), mockk(), defaultRepository))

    loadTestConfigurationRegistryData(appId, defaultRepository, configurationRegistry)

    return configurationRegistry
  }
}
