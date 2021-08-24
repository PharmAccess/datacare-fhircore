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

package org.smartregister.fhircore.util

import android.content.Context
import android.content.Intent
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import java.util.UUID
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RiskAssessment
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY

object QuestionnaireUtils {
  private val parser = FhirContext.forR4().newJsonParser()
  private val flaggableKey = "flag-detail"

  fun asPatientReference(id: String): Reference {
    return Reference().apply { this.reference = "Patient/$id" }
  }

  fun buildQuestionnaireIntent(
    context: Context,
    questionnaireTitle: String,
    questionnaireId: String,
    patientId: String?,
    isNewPatient: Boolean
  ): Intent {
    return Intent(context, QuestionnaireActivity::class.java).apply {
      putExtra(QUESTIONNAIRE_TITLE_KEY, questionnaireTitle)
      putExtra(QUESTIONNAIRE_PATH_KEY, questionnaireId)

      patientId?.let {
        if (isNewPatient) putExtra(QUESTIONNAIRE_ARG_PRE_ASSIGNED_ID, patientId)
        else putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
      }
    }
  }

  fun asQuestionnaireResponse(questionnaireResponse: String): QuestionnaireResponse {
    return parser.parseResource(questionnaireResponse) as QuestionnaireResponse
  }

  fun asCodeableConcept(linkId: String, q: Questionnaire): CodeableConcept {
    val qit = q.find(linkId)!!
    return asCodeableConcept(qit)
  }

  fun asCodeableConcept(qit: Questionnaire.QuestionnaireItemComponent): CodeableConcept {
    return CodeableConcept().apply {
      this.text = qit.text
      this.coding = qit.code

      this.addCoding().apply {
        this.code = qit.linkId
        this.system = qit.definition
      }
    }
  }

  fun asCodeableConcept(
    qit: QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
  ): CodeableConcept {
    return CodeableConcept().apply { this.addCoding(qit.valueCoding) }
  }

  fun asObs(
    qr: QuestionnaireResponse.QuestionnaireResponseItemComponent,
    subject: Patient,
    questionnaire: Questionnaire
  ): Observation {
    val obs = Observation()
    obs.id = getUniqueId()
    obs.effective = DateTimeType.now()
    obs.code = asCodeableConcept(qr.linkId, questionnaire)
    obs.status = Observation.ObservationStatus.FINAL
    obs.value = if (qr.hasAnswer()) qr.answer[0].value else null
    obs.subject = asPatientReference(subject.id)

    return obs
  }

  // todo revisit this logic when ResourceMapper is stable
  fun extractObservations(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    patient: Patient
  ): MutableList<Observation> {
    val observations = mutableListOf<Observation>()

    for (i in 0 until questionnaire.item.size) {
      // questionnaire and questionnaireResponse mapping go parallel
      val qItem = questionnaire.item[i]
      val qrItem = questionnaireResponse.item[i]

      if (qItem.definition?.contains("Observation") == true) {
        // get main group obs. only 1 level of obs nesting allowed for now //todo
        val main = asObs(qrItem, patient, questionnaire)

        // loop over all individual obs
        for (j in 0 until qrItem.item.size) {
          val mainRespItem = qrItem.item[j]

          if (mainRespItem.hasAnswer()) {
            val obs = asObs(mainRespItem, patient, questionnaire)

            // add reference to each comorbidity to main group obs
            main.addHasMember(Reference().apply { this.reference = "Observation/" + obs.id })

            observations.add(obs)
          }
        }

        observations.add(main)
      }
    }

    return observations
  }

  fun extractFlagExtension(item: Questionnaire.QuestionnaireItemComponent): Extension? {
    return item.extension.firstOrNull { it.url.contains(flaggableKey) }
  }

  fun extractFlagExtension(
    code: Coding,
    item: Questionnaire.QuestionnaireItemComponent
  ): Extension? {
    return item.extension.firstOrNull {
      it.url.contains(flaggableKey) &&
        (it.value.toString().contains(code.display) || code.display.contains(it.value.toString()))
    }
  }

  fun extractFlaggables(
    items: List<Questionnaire.QuestionnaireItemComponent>,
    target: MutableList<Questionnaire.QuestionnaireItemComponent>
  ) {
    items.forEach { qi ->
      qi.extension.firstOrNull { it.url.contains(flaggableKey) }?.let { target.add(qi) }

      if (qi.item.isNotEmpty()) {
        extractFlaggables(qi.item, target)
      }
    }
  }

  fun extractFlags(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    patient: Patient
  ): MutableList<Pair<Flag, Extension>> {
    val flaggableItems = mutableListOf<Questionnaire.QuestionnaireItemComponent>()

    extractFlaggables(questionnaire.item, flaggableItems)

    val flags = mutableListOf<Pair<Flag, Extension>>()

    flaggableItems.forEach { qi ->
      // only add flags where answer is true or answer code matches flag value
      questionnaireResponse.find(qi.linkId)?.answer?.firstOrNull { it.hasValue() }?.let {
        val flag = Flag()
        flag.id = getUniqueId()
        flag.status = Flag.FlagStatus.ACTIVE
        flag.subject = asPatientReference(patient.id)
        // todo simplify
        if (it.hasValueCoding() && extractFlagExtension(it.valueCoding, qi) != null) {
          flag.code = asCodeableConcept(it)
          val ext = extractFlagExtension(it.valueCoding, qi)

          flags.add(Pair(flag, ext!!))
        } else if (it.hasValueBooleanType() && it.valueBooleanType.booleanValue()) {
          flag.code = asCodeableConcept(qi)

          val ext = extractFlagExtension(qi)

          flags.add(Pair(flag, ext!!))
        }
      }
    }

    return flags
  }

  fun extractTags(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire
  ): MutableList<Coding> {
    val taggable = mutableListOf<Questionnaire.QuestionnaireItemComponent>()

    itemsWithDefinition("Patient.meta.tag", questionnaire.item, taggable)

    val tags = mutableListOf<Coding>()

    taggable.forEach { qi ->
      // only add flags where answer is true or answer code matches flag value
      questionnaireResponse
        .find(qi.linkId)
        ?.answer
        ?.firstOrNull { it.hasValue() }
        ?.let {
          when (it.value) {
            is Coding -> it.valueCoding // for coding tag with any option selected by user
            is BooleanType -> qi.code[0] // for boolean tag wih question code if true
            else -> null
          }
        }
        ?.let { tags.add(it) }
    }

    return tags
  }

  private fun itemsWithDefinition(
    definition: String,
    items: List<Questionnaire.QuestionnaireItemComponent>,
    target: MutableList<Questionnaire.QuestionnaireItemComponent>
  ) {
    items.forEach {
      if (it.definition?.contains(definition, true) == true) {
        target.add(it)
      }

      if (it.item.isNotEmpty()) {
        itemsWithDefinition(definition, it.item, target)
      }
    }
  }

  fun valueStringWithLinkId(questionnaireResponse: QuestionnaireResponse, linkId: String): String? {
    val ans = questionnaireResponse.find(linkId)?.answerFirstRep
    return ans?.valueStringType?.asStringValue()
  }

  private fun getItem(
    obs: Observation,
    item: Questionnaire.QuestionnaireItemComponent?
  ): Questionnaire.QuestionnaireItemComponent? {
    item ?: return null

    val codes = item.code.map { it.code }.toMutableList()
    codes.add(item.linkId)
    codes.add(item.text)

    if (obs.code.coding.any { codes.contains(it.code) }) {
      return item
    }

    item.item.forEach {
      val res = getItem(obs, it)
      if (res != null) {
        return res
      }
    }

    return null
  }

  // only one risk per questionnaire supported for now //todo
  fun extractRiskAssessment(
    observations: List<Observation>,
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire
  ): RiskAssessment? {
    val risks = mutableListOf<Questionnaire.QuestionnaireItemComponent>()
    itemsWithDefinition("RiskAssessment", questionnaire.item, risks)

    if (risks.isEmpty()) return null

    val qItem = risks[0]
    var qrItem = questionnaireResponse.find(qItem.linkId)

    var riskScore = 0
    val risk = RiskAssessment()

    observations.forEach { obs ->
      val qObs = questionnaire.item.map { getItem(obs, it) }.find { it != null }

      if (qObs != null) {
        val isRiskObs =
          qObs.extension.singleOrNull { ro -> ro.url.contains("RiskAssessment") } != null

        // todo revisit this when calculate expression is working
        if (isRiskObs &&
            ((obs.hasValueBooleanType() && obs.valueBooleanType.booleanValue()) ||
              (obs.hasValueStringType() && obs.hasValue()))
        ) {
          riskScore++

          risk.addBasis(Reference().apply { this.reference = "Observation/" + obs.id })
        }
      }
    }

    risk.status = RiskAssessment.RiskAssessmentStatus.FINAL
    risk.code = asCodeableConcept(qrItem!!.linkId, questionnaire)
    risk.id = getUniqueId()
    risk.subject = observations[0].subject
    risk.occurrence = DateTimeType.now()
    risk.addPrediction().apply {
      this.relativeRisk = riskScore.toBigDecimal()

      // todo change when calculated expression is working
      if (qrItem.hasAnswer() && riskScore > 0) {
        this.outcome =
          CodeableConcept().apply {
            this.text = qrItem.answer[0].valueCoding.display
            this.coding = listOf(qrItem.answer[0].valueCoding)
          }
      }
    }

    return risk
  }

  private fun getUniqueId(): String {
    return UUID.randomUUID().toString()
  }
}