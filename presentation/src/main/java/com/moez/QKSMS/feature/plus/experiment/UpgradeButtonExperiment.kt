/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.plus.experiment

import android.content.Context
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.experiment.Experiment
import org.prauga.messages.experiment.Variant

class UpgradeButtonExperiment @Inject constructor(context: Context) : Experiment<Int>(context) {

    override val key: String = "Upgrade Button"

    override val variants: List<Variant<Int>> =
        listOf(
            Variant("variant_a", R.string.qksms_plus_upgrade),
            Variant("variant_b", R.string.qksms_plus_upgrade_b),
            Variant("variant_c", R.string.qksms_plus_upgrade_c),
            Variant("variant_d", R.string.qksms_plus_upgrade_d),
        )

    override val default: Int = R.string.qksms_plus_upgrade
}
