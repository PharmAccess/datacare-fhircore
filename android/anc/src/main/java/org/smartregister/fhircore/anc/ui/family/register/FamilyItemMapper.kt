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

package org.smartregister.fhircore.anc.ui.family.register

import com.google.android.fhir.logicalId
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.codesystems.RequestStatus
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.atRisk
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.isPregnant

object OpenFamilyProfile : ListenerIntent

data class Family(val head: Patient, val members: List<Patient>, val servicesDue: List<CarePlan>)

object FamilyItemMapper : DomainMapper<Family, FamilyItem> {

  override fun mapToDomainModel(dto: Family): FamilyItem {
    val head = dto.head
    val members = dto.members
    val servicesDue = dto.servicesDue

    return FamilyItem(
      id = head.logicalId,
      name = head.extractName(),
      gender = head.extractGender(),
      age = head.extractAge(),
      address = head.extractAddress(),
      isPregnant = head.isPregnant(),
      members = members.map { toFamilyMemberItem(it) },
      servicesDue = servicesDue.filter { it.status.equals(RequestStatus.ACTIVE) && it.period.hasStart() }.size,
      servicesOverdue = servicesDue.filter { it.status.equals(RequestStatus.ACTIVE) && it.period.hasEnd() }.size
    )
  }

  fun toFamilyMemberItem(member: Patient): FamilyMemberItem {
    return FamilyMemberItem(
      id = member.logicalId,
      age = member.extractAge(),
      gender = member.extractGender(),
      pregnant = member.isPregnant()
    )
  }

  override fun mapFromDomainModel(domainModel: FamilyItem): Family {
    throw UnsupportedOperationException()
  }
}