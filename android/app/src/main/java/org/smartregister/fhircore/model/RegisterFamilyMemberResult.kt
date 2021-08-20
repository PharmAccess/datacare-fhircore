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

package org.smartregister.fhircore.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_ACTIVITY_RESULT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY
import org.smartregister.fhircore.util.QuestionnaireUtils

data class RegisterFamilyMemberData(val headId: String, val familyDetailView: FamilyDetailView)

class RegisterFamilyMemberResult : ActivityResultContract<RegisterFamilyMemberData, String?>() {

  override fun createIntent(context: Context, input: RegisterFamilyMemberData): Intent {
    val intent =
      QuestionnaireUtils.buildQuestionnaireIntent(
        context,
        input.familyDetailView.memberRegistrationQuestionnaireTitle,
        input.familyDetailView.memberRegistrationQuestionnaireIdentifier,
        null,
        true
      )
    intent.putExtra(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY, input.headId)
    return intent
  }

  override fun parseResult(resultCode: Int, intent: Intent?): String? {
    val data = intent?.getBundleExtra(QUESTIONNAIRE_ARG_ACTIVITY_RESULT_KEY)

    return if (resultCode == Activity.RESULT_OK && data != null) {
      return data?.getString(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY)
    } else null
  }
}
