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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.databinding.ActivityAncDetailsBinding
import org.smartregister.fhircore.anc.ui.madx.details.NonAncDetailsActivity
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

class AncDetailsActivity : BaseMultiLanguageActivity() {

    private lateinit var patientId: String

    private lateinit var activityAncDetailsBinding: ActivityAncDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityAncDetailsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_anc_details)
        setSupportActionBar(activityAncDetailsBinding.patientDetailsToolbar)

//        if (savedInstanceState == null) {
            patientId =
                intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    AncDetailsFragment.newInstance(
                        bundleOf(
                            Pair(
                                QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY,
                                patientId
                            )
                        )
                    )
                )
                .commitNow()
//        }

        activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val removeThisPerson = menu!!.findItem(R.id.remove_this_person)
        val markAsAncClient = menu!!.findItem(R.id.mark_as_anc_client)
        val addVitals = menu!!.findItem(R.id.add_vitals)
        val addConditions = menu!!.findItem(R.id.add_conditions)
        val viewPastEncounters = menu!!.findItem(R.id.view_past_encounters)
        val bmiWidget = menu!!.findItem(R.id.bmi_widget)

        viewPastEncounters.isVisible = true
        markAsAncClient.isVisible = false
        addVitals.isVisible = true
        addConditions.isVisible = false
        bmiWidget.isVisible = false

        val title = removeThisPerson.title.toString()
        val s = SpannableString(title)
        with(s) {
            setSpan(
                ForegroundColorSpan(android.graphics.Color.parseColor("#DD0000")),
                0,
                length,
                android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        } // provide whatever color you want here.
        removeThisPerson.title = s
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_vitals) {
            startActivity(
                Intent(this, NonAncDetailsActivity::class.java).apply {
                    putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY,patientId)
                }
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
