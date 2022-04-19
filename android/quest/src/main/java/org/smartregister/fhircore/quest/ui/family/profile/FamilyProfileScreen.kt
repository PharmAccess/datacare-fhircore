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

package org.smartregister.fhircore.quest.ui.family.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.family.profile.components.FamilyProfileRow
import org.smartregister.fhircore.quest.ui.family.profile.components.FamilyProfileTopBar

@Composable
fun FamilyProfileScreen(
  patientId: String?,
  navController: NavHostController,
  modifier: Modifier = Modifier,
  familyProfileViewModel: FamilyProfileViewModel = hiltViewModel()
) {

  LaunchedEffect(Unit) { familyProfileViewModel.fetchFamilyProfileData(patientId) }

  val viewState = familyProfileViewModel.familyProfileUiState.value

  val profileViewData = familyProfileViewModel.familyMemberProfileData.value

  var showOverflowMenu by remember { mutableStateOf(false) }

  val mutableInteractionSource = remember { MutableInteractionSource() }

  val verticalScrollState = rememberScrollState()

  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
        backgroundColor = MaterialTheme.colors.primary,
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
          }
        },
        elevation = 0.dp, // No elevation to remove drop shadow
        actions = {
          IconButton(onClick = { showOverflowMenu = !showOverflowMenu }) {
            Icon(
              imageVector = Icons.Outlined.MoreVert,
              contentDescription = null,
              tint = Color.White
            )
          }
          DropdownMenu(
            expanded = showOverflowMenu,
            onDismissRequest = { showOverflowMenu = false },
            modifier = modifier.padding(0.dp)
          ) {
            viewState.overflowMenuItems.forEach {
              DropdownMenuItem(
                onClick = {
                  showOverflowMenu = false
                  familyProfileViewModel.onEvent(FamilyProfileEvent.OverflowMenuClick(it.id))
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier =
                  modifier
                    .fillMaxWidth()
                    .background(
                      color =
                        if (it.confirmAction) it.titleColor.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
              ) { Text(text = stringResource(it.titleResource), color = it.titleColor) }
            }
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        contentColor = Color.White,
        text = { Text(text = stringResource(R.string.add_memeber).uppercase()) },
        onClick = {
          familyProfileViewModel.onEvent(FamilyProfileEvent.AddMember(context, patientId))
        },
        backgroundColor = MaterialTheme.colors.primary,
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        interactionSource = mutableInteractionSource
      )
    }
  ) { innerPadding ->
    Box(modifier = modifier.padding(innerPadding)) {
      Column(modifier = modifier.verticalScroll(verticalScrollState)) {
        // Appbar section
        FamilyProfileTopBar(profileViewData, modifier)

        // Household visit section
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
          Text(text = stringResource(R.string.household), fontSize = 18.sp)
          OutlinedButton(
            onClick = { familyProfileViewModel.onEvent(FamilyProfileEvent.RoutineVisit) },
            colors =
              ButtonDefaults.buttonColors(
                backgroundColor = InfoColor.copy(alpha = 0.1f),
                contentColor = InfoColor,
              )
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Filled.Add, contentDescription = null)
              Text(text = stringResource(R.string.routine_visit))
            }
          }
        }

        Divider()

        // Family members section
        profileViewData.familyMemberViewStates.forEach { memberViewState ->
          FamilyProfileRow(
            familyMemberViewState = memberViewState,
            onFamilyMemberClick = {
              familyProfileViewModel.onEvent(
                FamilyProfileEvent.MemberClick(memberViewState.patientId)
              )
            },
            onTaskClick = { taskFormId ->
              familyProfileViewModel.onEvent(FamilyProfileEvent.OpenTaskForm(taskFormId))
            }
          )
          Divider()
        }
      }
    }
  }
}