/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.feature.parcel

import io.reactivex.Observable
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.repository.ParcelCodeRepository
import javax.inject.Inject

class ParcelCodePresenter @Inject constructor(
    private val parcelCodeRepository: ParcelCodeRepository
) : QkPresenter<ParcelCodeView, ParcelCodeState>(ParcelCodeState()) {

    override fun bindIntents(view: ParcelCodeView) {
        super.bindIntents(view)

        // 加载取件码列表
        val parcelCodes = parcelCodeRepository.getParcelCodes()
        disposables += parcelCodes
                .subscribe { codes -> newState { copy(parcelCodes = codes, isLoading = false) } }
    }

}
