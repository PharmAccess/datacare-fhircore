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

package org.smartregister.fhircore.quest.ui.family.profile

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.patient.PatientRegisterRepository
import org.smartregister.fhircore.engine.navigation.OverflowMenuFactory
import org.smartregister.fhircore.engine.navigation.OverflowMenuHost
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.patient.profile.model.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper

@HiltViewModel
class FamilyProfileViewModel
@Inject
constructor(
  val overflowMenuFactory: OverflowMenuFactory,
  val patientRegisterRepository: PatientRegisterRepository,
  val profileViewDataMapper: ProfileViewDataMapper,
  val dispatcherProvider: DefaultDispatcherProvider
) : ViewModel() {

  val familyProfileUiState: MutableState<FamilyProfileUiState> =
    mutableStateOf(
      FamilyProfileUiState(
        overflowMenuItems =
          overflowMenuFactory.overflowMenuMap.getValue(OverflowMenuHost.FAMILY_PROFILE)
      )
    )

  val familyMemberProfileData: MutableState<ProfileViewData.FamilyProfileViewData> =
    mutableStateOf(ProfileViewData.FamilyProfileViewData())

  fun onEvent(event: FamilyProfileEvent) {
    when (event) {
      is FamilyProfileEvent.AddMember -> {
        event.context.launchQuestionnaireActivity(questionnaireId = FAMILY_MEMBER_REGISTER_FORM)
      }
    }
  }

  fun fetchFamilyProfileData(patientId: String?) {
    viewModelScope.launch(dispatcherProvider.io()) {
      if (!patientId.isNullOrEmpty()) {
        patientRegisterRepository.loadPatientProfileData(
            AppFeature.HouseholdManagement.name,
            HealthModule.FAMILY,
            patientId
          )
          ?.let {
            familyMemberProfileData.value =
              profileViewDataMapper.transformInputToOutputModel(it) as
                ProfileViewData.FamilyProfileViewData
          }
      }
    }
  }

  companion object {
    const val FAMILY_MEMBER_REGISTER_FORM = "family-member-registration"
  }
}