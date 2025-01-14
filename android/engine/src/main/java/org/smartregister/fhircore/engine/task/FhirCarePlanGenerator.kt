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

package org.smartregister.fhircore.engine.task

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.ActivityDefinition
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import timber.log.Timber

@Singleton
class FhirCarePlanGenerator
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val fhirPathEngine: FHIRPathEngine,
  val transformSupportServices: TransformSupportServices,
  val defaultRepository: DefaultRepository,
  val workManager: WorkManager
) {
  val structureMapUtilities by lazy {
    StructureMapUtilities(transformSupportServices.simpleWorkerContext, transformSupportServices)
  }

  suspend fun generateOrUpdateCarePlan(
    planDefinitionId: String,
    subject: Resource,
    data: Bundle? = null
  ): CarePlan? {
    return generateOrUpdateCarePlan(fhirEngine.get(planDefinitionId), subject, data)
  }

  suspend fun generateOrUpdateCarePlan(
    planDefinition: PlanDefinition,
    subject: Resource,
    data: Bundle? = null
  ): CarePlan? {
    // Only one CarePlan per plan , update or init a new one if not exists
    val output =
      fhirEngine
        .search<CarePlan> {
          filter(CarePlan.INSTANTIATES_CANONICAL, { value = planDefinition.referenceValue() })
        }
        .firstOrNull()
        ?: CarePlan().apply {
          this.title = planDefinition.title
          this.description = planDefinition.description
          this.instantiatesCanonical = listOf(CanonicalType(planDefinition.asReference().reference))
        }

    var carePlanModified = false

    planDefinition.action.forEach { action ->
      val input = Bundle().apply { entry.addAll(data?.entry ?: listOf()) }

      if (action.condition.all {
          if (it.kind != PlanDefinition.ActionConditionKind.APPLICABILITY)
            throw UnsupportedOperationException(
              "PlanDefinition.action.kind=${it.kind} not supported"
            )

          if (it.expression.language != Expression.ExpressionLanguage.TEXT_FHIRPATH.toCode())
            throw UnsupportedOperationException(
              "PlanDefinition.expression.language=${it.expression.language} not supported"
            )

          fhirPathEngine.evaluateToBoolean(input, null, subject, it.expression.expression)
        }
      ) {
        val definition =
          planDefinition.contained.first {
            it.id.substringBefore("/_history") == action.definitionCanonicalType.value
          } as
            ActivityDefinition

        val source =
          Parameters().apply {
            addParameter(
              Parameters.ParametersParameterComponent().apply {
                this.name = CarePlan.SP_SUBJECT
                this.resource = subject
              }
            )

            addParameter(
              Parameters.ParametersParameterComponent().apply {
                this.name = PlanDefinition.SP_DEFINITION
                this.resource = definition
              }
            )

            // TODO find some other way (activity definition based) to pass additional data
            addParameter(
              Parameters.ParametersParameterComponent().apply {
                this.name = PlanDefinition.SP_DEPENDS_ON
                this.resource = data
              }
            )
          }

        if (action.hasTransform()) {
          val structureMap = fhirEngine.get<StructureMap>(IdType(action.transform).idPart)
          structureMapUtilities.transform(
            transformSupportServices.simpleWorkerContext,
            source,
            structureMap,
            output
          )
        }

        if (definition.hasDynamicValue()) {
          definition.dynamicValue.forEach { dynamicValue ->
            if (definition.kind == ActivityDefinition.ActivityDefinitionKind.CAREPLAN)
              dynamicValue.expression.expression
                .let { fhirPathEngine.evaluate(null, source, null, subject, it).firstOrNull() }
                ?.run {
                  output.setPropertySafely(
                    dynamicValue.path.removePrefix("${definition.kind.toCode()}."),
                    this
                  )
                }
            else throw UnsupportedOperationException("${definition.kind} not supported")
          }
        }
        carePlanModified = true
      }
    }

    if (carePlanModified) saveCarePlan(output)

    // Schedule onetime immediate job that updates the status of the tasks
    workManager.enqueue(OneTimeWorkRequestBuilder<FhirTaskPlanWorker>().build())

    return if (output.hasActivity()) output else null
  }

  private suspend fun saveCarePlan(output: CarePlan) {
    output.also { Timber.d(it.encodeResourceToString()) }.also { carePlan ->
      // Save embedded resources inside as independent entries, clear embedded and save carePlan
      val dependents = carePlan.contained.map { it }

      carePlan.contained.clear()

      // Save CarePlan only if it has activity, otherwise just save contained/dependent resources
      if (output.hasActivity()) defaultRepository.create(true, carePlan)

      dependents.forEach { defaultRepository.create(true, it) }

      if (carePlan.status == CarePlan.CarePlanStatus.COMPLETED)
        carePlan
          .activity
          .flatMap { it.outcomeReference }
          .filter { it.reference.startsWith(ResourceType.Task.name) }
          .mapNotNull { getTask(it.extractId()) }
          .forEach {
            if (it.status.isIn(TaskStatus.REQUESTED, TaskStatus.READY, TaskStatus.INPROGRESS)) {
              cancelTask(it.logicalId, "${carePlan.fhirType()} ${carePlan.status}")
            }
          }
    }
  }

  suspend fun transitionTaskTo(id: String, status: TaskStatus) {
    getTask(id)
      ?.apply {
        this.status = status
        this.lastModified = Date()
      }
      ?.run { defaultRepository.addOrUpdate(addMandatoryTags = true, resource = this) }
  }

  suspend fun cancelTask(id: String, reason: String) {
    getTask(id)
      ?.apply {
        this.status = TaskStatus.CANCELLED
        this.lastModified = Date()
        this.statusReason = CodeableConcept().apply { text = reason }
      }
      ?.run { defaultRepository.addOrUpdate(addMandatoryTags = true, resource = this) }
  }

  suspend fun getTask(id: String) =
    kotlin.runCatching { fhirEngine.get<Task>(id) }.onFailure { Timber.e(it) }.getOrNull()
}
