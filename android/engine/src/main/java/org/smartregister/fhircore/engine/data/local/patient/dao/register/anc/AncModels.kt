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

package org.smartregister.fhircore.engine.data.local.patient.dao.register.anc

import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task

data class Anc(
  val patient: Patient,
  val conditions: List<Condition> = listOf(),
  val carePlans: List<CarePlan> = listOf()
)

data class AncProfile(
  val patient: Patient,
  val conditions: List<Condition> = listOf(),
  val carePlans: List<CarePlan> = listOf(),
  val tasks: List<Task> = listOf(),
  val flags: List<Flag> = listOf(),
  val encounters: List<Encounter> = listOf()
)