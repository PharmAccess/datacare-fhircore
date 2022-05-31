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

package org.smartregister.fhircore.engine.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.LoginViewConfiguration
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme

@AndroidEntryPoint
class LoginActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<LoginViewConfiguration> {

  @Inject lateinit var loginService: LoginService

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var syncBroadcaster: Lazy<SyncBroadcaster>

  private val loginViewModel by viewModels<LoginViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loginService.loginActivity = this

    if (configurationRegistry.isAppIdInitialized()) {
      configureViews(configurationRegistry.retrieveConfiguration(AppConfigClassification.LOGIN))
    }

    loginViewModel.apply {
      // Run sync and navigate directly to home screen if session is active
      if (accountAuthenticator.hasActiveSession()) runSyncAndNavigateHome()

      val isPinEnabled = loginViewModel.loginViewConfiguration.value?.enablePin ?: false
      navigateToHome.observe(this@LoginActivity) { launchHomeScreen ->
        when {
          launchHomeScreen && isPinEnabled && accountAuthenticator.hasActivePin() -> {
            loginService.navigateToPinLogin(false)
          }
          launchHomeScreen && isPinEnabled && !accountAuthenticator.hasActivePin() -> {
            loginService.navigateToPinLogin(true)
          }
          launchHomeScreen && !isPinEnabled -> {
            runSyncAndNavigateHome()
          }
        }
      }
      launchDialPad.observe(this@LoginActivity) { if (!it.isNullOrEmpty()) launchDialPad(it) }
    }

    setContent { AppTheme { LoginScreen(loginViewModel = loginViewModel) } }
  }

  private fun runSyncAndNavigateHome() {
    loginService.navigateToHome()
    syncBroadcaster.get().runSync()
  }

  fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)
  }

  override fun configureViews(viewConfiguration: LoginViewConfiguration) {
    loginViewModel.updateViewConfigurations(viewConfiguration)
  }

  private fun launchDialPad(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse(phone) })
  }
}
