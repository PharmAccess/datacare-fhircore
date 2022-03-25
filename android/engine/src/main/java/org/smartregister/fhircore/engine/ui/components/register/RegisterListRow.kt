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

package org.smartregister.fhircore.engine.ui.components.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.domain.model.RegisterRow

@Composable
fun RegisterListRow(modifier: Modifier = Modifier, registerRow: RegisterRow) {
  Column(
    modifier =
      modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 16.dp, vertical = 24.dp)
  ) { Text(text = registerRow.name) }
}

@Composable
@Preview(showBackground = true)
fun RegisterListRowPreview() {
  RegisterListRow(
    registerRow =
      RegisterRow(
        identifier = "1234",
        logicalId = "1234",
        name = "John Doe",
        gender = "Male",
        address = "Nairobi"
      )
  )
}