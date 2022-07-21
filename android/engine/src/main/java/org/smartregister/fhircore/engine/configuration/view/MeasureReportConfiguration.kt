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

package org.smartregister.fhircore.engine.configuration.view

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
@Serializable
class MeasureReportConfiguration(
  override val appId: String = "",
  override val classification: String = "",
  val reports: List<MeasureReportRowConfig> = listOf()
) : Configuration

@Stable
@Serializable
data class MeasureReportRowConfig(
  val id: String = "",
  val title: String = "",
  val description: String = "",
  val reportType: String = "",
  val name: String = ""
)

@Stable
fun measureReportConfigurationOf(
  appId: String = "",
  classification: String = "report",
  reports: List<MeasureReportRowConfig> = listOf()
) = MeasureReportConfiguration(appId = appId, classification = classification, reports = reports)