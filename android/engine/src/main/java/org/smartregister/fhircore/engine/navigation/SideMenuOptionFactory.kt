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

package org.smartregister.fhircore.engine.navigation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.patient.PatientRepository
import org.smartregister.fhircore.engine.domain.model.SideMenuOption

@Singleton
class SideMenuOptionFactory
@Inject
constructor(val appFeatureManager: AppFeatureManager, val patientRepository: PatientRepository) {
  val defaultSideMenu =
    SideMenuOption(
      appFeatureName = AppFeature.PatientManagement.name,
      healthModule = null,
      iconResource = R.drawable.ic_baby_mother,
      titleResource = R.string.all_clients,
      showCount = true,
      count =
        runBlocking {
          patientRepository.countRegisterData(
            appFeatureName = AppFeature.PatientManagement.name,
            healthModule = null
          )
        }
    )

  fun retrieveSideMenuOptions(): List<SideMenuOption> {
    val sideMenuOptions =
      appFeatureManager.activeRegisterFeatures().map {
        SideMenuOption(
          appFeatureName = it.feature,
          healthModule = it.healthModule,
          iconResource = R.drawable.ic_baby_mother,
          titleResource =
            when (it.healthModule) {
              HealthModule.ANC -> R.string.anc_clients
              HealthModule.RDT -> R.string.all_clients
              HealthModule.PNC -> R.string.pnc_clients
              HealthModule.FAMILY -> R.string.families
              HealthModule.CHILD -> R.string.children
              HealthModule.FAMILY_PLANNING -> R.string.family_planning_clients
            },
          showCount = true,
          count =
            runBlocking {
              patientRepository.countRegisterData(
                appFeatureName = AppFeature.PatientManagement.name,
                healthModule = it.healthModule
              )
            }
        )
      }
    return sideMenuOptions.ifEmpty { listOf(defaultSideMenu) }
  }
}