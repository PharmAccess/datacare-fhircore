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

package org.smartregister.fhircore.quest.di.modules

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import org.smartregister.fhircore.engine.di.DispatcherModule
import org.smartregister.fhircore.engine.di.FhirEngineModule
import org.smartregister.fhircore.engine.di.NetworkModule

@Module(includes = [NetworkModule::class, DispatcherModule::class])
@TestInstallIn(components = [SingletonComponent::class], replaces = [FhirEngineModule::class])
class FakeFhirEngineModule {

  @Provides
  @Singleton
  fun provideFhirEngine(@ApplicationContext context: Context): FhirEngine {
    return FhirEngineProvider.getInstance(context)
  }
}