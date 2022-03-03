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

package org.smartregister.fhircore.engine.cql

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.ref.SoftReference
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.smartregister.fhircore.engine.util.FileUtil

@Singleton
class CqlLibraryHelper @Inject constructor(@ApplicationContext private val context: Context) {

  private val fhirContext: FhirContext = FhirContext.forCached(FhirVersionEnum.R4)
  private val parser = fhirContext.newJsonParser()
  private var libraryMeasure: SoftReference<IBaseBundle> = SoftReference(null)

  fun loadMeasureEvaluateLibrary(
    fileNameMeasureLibraryCql: String,
    dirCqlDirRoot: String
  ): IBaseBundle {
    return libraryMeasure.get()
      ?: kotlin.run {
        val measureEvaluateLibraryData =
          FileUtil.readFileFromInternalStorage(context, fileNameMeasureLibraryCql, dirCqlDirRoot)
        val libraryStreamMeasure: InputStream =
          ByteArrayInputStream(measureEvaluateLibraryData.toByteArray())

        libraryMeasure = SoftReference(parser.parseResource(libraryStreamMeasure) as IBaseBundle)
        libraryMeasure.get()!!
      }
  }

  fun writeMeasureEvaluateLibraryData(
    measureEvaluateLibraryData: String,
    fileNameMeasureLibraryCql: String,
    dirCqlDirRoot: String
  ) {
    FileUtil.writeFileOnInternalStorage(
      context,
      fileNameMeasureLibraryCql,
      measureEvaluateLibraryData,
      dirCqlDirRoot
    )
  }
}
