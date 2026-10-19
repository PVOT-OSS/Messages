/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.main

import android.content.Context
import javax.inject.Inject
import org.prauga.messages.experiment.Experiment
import org.prauga.messages.experiment.Variant

class DrawerBadgesExperiment @Inject constructor(context: Context) : Experiment<Boolean>(context) {

    override val key: String = "Drawer Badges"

    override val variants: List<Variant<Boolean>> =
        listOf(Variant("variant_a", false), Variant("variant_b", true))

    override val default: Boolean = false

    override val qualifies: Boolean = true
}
