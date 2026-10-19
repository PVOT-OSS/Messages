/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.gallery

import io.reactivex.Observable
import org.prauga.messages.common.base.QkView
import org.prauga.messages.model.MmsPart

interface GalleryView : QkView<GalleryState> {

    fun optionsItemSelected(): Observable<Int>

    fun screenTouched(): Observable<*>

    fun pageChanged(): Observable<MmsPart>

    fun requestStoragePermission()
}
