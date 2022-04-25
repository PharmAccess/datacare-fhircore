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

package org.smartregister.fhircore.engine.data.local.register.dao

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.appfeature.model.HealthModule.FAMILY
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.FamilyMemberProfileData
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.DAYS_IN_YEAR
import org.smartregister.fhircore.engine.util.extension.daysPassed
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractDeathDate
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.extension.lastSeenFormat
import org.smartregister.fhircore.engine.util.extension.overdue
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
import org.smartregister.fhircore.engine.util.extension.toCoding

@Singleton
class FamilyRegisterDao
@Inject
constructor(
    val fhirEngine: FhirEngine,
    val defaultRepository: DefaultRepository,
    val configurationRegistry: ConfigurationRegistry
) : RegisterDao {

    override suspend fun loadRegisterData(
        currentPage: Int,
        loadAll: Boolean,
        appFeatureName: String?
    ): List<RegisterData> {
        val families =
            fhirEngine.search<Group> {
                getRegisterDataFilters(FAMILY.name).forEach { filterBy(it) }
                count =
                    if (loadAll) countRegisterData(appFeatureName).toInt()
                    else PaginationConstant.DEFAULT_PAGE_SIZE
                from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
            }

        return families.filter { it.active && !it.name.isNullOrEmpty() }.map { family ->
            val members =
                family.member?.map { member ->
                    defaultRepository
                        .loadResource<Patient>(member.entity.extractId())!!
                        .let { patient ->
                            loadFamilyMemberRegisterData(patient.logicalId)
                        }
                }
                    ?: listOf()
            val familyHead = loadFamilyHead(family)

            RegisterData.FamilyRegisterData(
                id = family.logicalId,
                name = family.name,
                identifier =
                family.identifier
                    .firstOrNull {
                        it.use.name.contentEquals(Identifier.IdentifierUse.OFFICIAL.toCode(), true)
                    }
                    ?.value,
                address = familyHead?.extractAddress() ?: "",
                head = familyHead?.let { loadFamilyMemberRegisterData(familyHead.logicalId) },
                members = members,
                servicesDue = members.sumOf { it.servicesDue ?: 0 },
                servicesOverdue = members.sumOf { it.servicesOverdue ?: 0 },
                lastSeen = family.meta?.lastUpdated.lastSeenFormat()
            )
        }
    }

    override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData {
        val family = defaultRepository.loadResource<Group>(resourceId)!!
        val familyHead = loadFamilyHead(family)

        return ProfileData.FamilyProfileData(
            id = family.logicalId,
            name = family.name,
            identifier =
            family.identifier
                .firstOrNull {
                    it.use.name.contentEquals(Identifier.IdentifierUse.OFFICIAL.toCode(), true)
                }
                ?.value,
            address = familyHead?.extractAddress() ?: "",
            head = familyHead?.let { loadFamilyMemberProfileData(familyHead.logicalId) },
            members =
            family.member?.map { member ->
                loadFamilyMemberProfileData(
                    defaultRepository
                        .loadResource<Patient>(member.entity.extractId())!!
                        .logicalId
                )
            }
                ?: listOf(),
            services =
            defaultRepository.searchResourceFor<CarePlan>(
                subjectId = family.logicalId,
                subjectParam = CarePlan.SUBJECT,
                filters = getRegisterDataFilters(FAMILY_CARE_PLAN)
            ),
            tasks =
            defaultRepository.searchResourceFor<Task>(
                subjectId = family.logicalId,
                subjectParam = Task.SUBJECT,
                subjectType = ResourceType.Group
            )
        )
    }

    override suspend fun countRegisterData(appFeatureName: String?): Long {
        // TODO fix this workaround for groups count
        return fhirEngine
            .search<Group> { getRegisterDataFilters(FAMILY.name).forEach { filterBy(it) } }
            .filter { it.active && !it.name.isNullOrEmpty() }
            .size
            .toLong()
    }

    private suspend fun loadFamilyHead(family: Group) =
        family.managingEntity?.let { reference ->
            fhirEngine
                .search<RelatedPerson> {
                    filterByResourceTypeId(
                        RelatedPerson.RES_ID,
                        ResourceType.RelatedPerson,
                        reference.extractId()
                    )
                }
                .firstOrNull()
                ?.let { relatedPerson ->
                    fhirEngine
                        .search<Patient> {
                            filterByResourceTypeId(
                                Patient.RES_ID,
                                ResourceType.Patient,
                                relatedPerson.patient.extractId()
                            )
                        }
                        .firstOrNull()
                }
        }

    private suspend fun loadFamilyMemberRegisterData(memberId: String) =
        defaultRepository.loadResource<Patient>(memberId)!!.let { patient ->
            val conditions = loadMemberCondition(patient.logicalId)
            val carePlans = loadMemberCarePlan(patient.logicalId)

            RegisterData.FamilyMemberRegisterData(
                id = patient.logicalId,
                name = patient.extractName(),
                age = (patient.birthDate.daysPassed() / DAYS_IN_YEAR).toString(),
                birthdate = patient.birthDate,
                gender = patient.gender.display.first().toString(),
                pregnant = conditions.hasActivePregnancy(),
                isHead = patient.isFamilyHead(),
                deathDate = patient.extractDeathDate(),
                servicesDue = carePlans.filter { it.due() }.flatMap { it.activity }.size,
                servicesOverdue = carePlans.filter { it.overdue() }.flatMap { it.activity }.size
            )
        }

    private suspend fun loadFamilyMemberProfileData(memberId: String) =
        defaultRepository.loadResource<Patient>(memberId)!!.let { patient ->
            val conditions = loadMemberCondition(patient.logicalId)
            val carePlans = loadMemberCarePlan(patient.logicalId)
            val tasks = loadMemberTask(patient.logicalId)
            val flags = loadMemberFlags(patient.logicalId)

            FamilyMemberProfileData(
                id = patient.logicalId,
                name = patient.extractName(),
                age = patient.birthDate.toAgeDisplay(),
                birthdate = patient.birthDate,
                gender = patient.gender,
                pregnant = conditions.hasActivePregnancy(),
                isHead = patient.isFamilyHead(),
                deathDate = patient.extractDeathDate(),
                conditions = conditions,
                flags = flags,
                services = carePlans,
                tasks = tasks
            )
        }

    private suspend fun loadMemberCondition(patientId: String) =
        defaultRepository
            .searchResourceFor<Condition>(
                subjectId = patientId,
                subjectParam = Condition.SUBJECT,
                subjectType = ResourceType.Patient
            )

    private suspend fun loadMemberCarePlan(patientId: String) =
        fhirEngine.search<CarePlan> {
            filterByResourceTypeId(CarePlan.SUBJECT, ResourceType.Patient, patientId)
            filter(CarePlan.STATUS, { value = of(CarePlan.CarePlanStatus.ACTIVE.toCoding()) })
        }

    private suspend fun loadMemberTask(patientId: String) =
        defaultRepository
            .searchResourceFor<Task>(
                subjectId = patientId,
                subjectParam = Task.SUBJECT,
                subjectType = ResourceType.Patient
            )

    private suspend fun loadMemberFlags(patientId: String) =
        defaultRepository
            .searchResourceFor<Flag>(
                subjectId = patientId,
                subjectParam = Flag.SUBJECT,
                subjectType = ResourceType.Patient
            )

    private fun getRegisterDataFilters(id: String) =
        configurationRegistry.retrieveDataFilterConfiguration(id)

    companion object {
        const val FAMILY_CARE_PLAN = "family_care_plan"
    }
}