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

package org.smartregister.fhircore.quest.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.State
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirTaskGenerator
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

@AndroidEntryPoint
open class AppMainActivity : BaseMultiLanguageActivity(), OnSyncListener {

  @Inject lateinit var syncBroadcaster: SyncBroadcaster
  @Inject lateinit var fhirTaskGenerator: FhirTaskGenerator

  val appMainViewModel by viewModels<AppMainViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { AppTheme { MainScreen(appMainViewModel = appMainViewModel) } }
    syncBroadcaster.registerSyncListener(this, lifecycleScope)
  }

  override fun onSync(state: State) {
    Timber.i("Sync state received is $state")
    when (state) {
      is State.Started -> {
        showToast(getString(R.string.syncing))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(getString(R.string.syncing_initiated))
        )
      }
      is State.InProgress -> {
        Timber.d("Syncing in progress: Resource type ${state.resourceType?.name}")
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(getString(R.string.syncing_in_progress))
        )
      }
      is State.Glitch -> {
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(appMainViewModel.retrieveLastSyncTimestamp())
        )
        Timber.w(state.exceptions.joinToString { it.exception.message.toString() })
      }
      is State.Failed -> {
        showToast(getString(R.string.sync_failed))
        appMainViewModel.onEvent(
          AppMainEvent.UpdateSyncState(
            appMainViewModel.retrieveLastSyncTimestamp() ?: getString(R.string.syncing_failed)
          )
        )
        Timber.e(state.result.exceptions.joinToString { it.exception.message.toString() })
      }
      is State.Finished -> {
        showToast(getString(R.string.sync_completed))
        appMainViewModel.run {
          onEvent(
            AppMainEvent.UpdateSyncState(
              getString(
                R.string.last_sync_timestamp,
                formatLastSyncTimestamp(state.result.timestamp)
              )
            )
          )
          updateLastSyncTimestamp(state.result.timestamp)

          tempMethodToUpdatePeriodPlanWorkerAndFhirGen()
        }
      }
    }
  }

  // TODO move to where required.. Elly
  fun tempMethodToUpdatePeriodPlanWorkerAndFhirGen() {
    Timber.e("Registering plan and task scheduler")
    with(appMainViewModel.configService) {
      if (true /*registerViewModel.applicationConfiguration.scheduleDefaultPlanWorker*/)
        this.schedulePlan(this@AppMainActivity)
      else this.unschedulePlan(this@AppMainActivity)
    }

    runBlocking {
      fhirTaskGenerator.generateCarePlan(
        "105121",
        Patient().apply {
          birthDate = Date().plusYears(-5)
          id =
            fhirTaskGenerator
              .fhirEngine
              .search<Patient> { filter(Patient.ACTIVE, { value = of(true) }) }
              .last()
              .id
        }
      )
    }
  }
}