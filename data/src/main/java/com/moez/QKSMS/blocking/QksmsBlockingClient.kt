/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.blocking

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import org.prauga.messages.repository.BlockingRepository

class QksmsBlockingClient @Inject constructor(private val blockingRepo: BlockingRepository) :
    BlockingClient {

    override fun isAvailable(): Boolean = true

    override fun getClientCapability() = BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION

    override fun shouldBlock(address: String): Single<BlockingClient.Action> =
        isBlacklisted(address)

    override fun isBlacklisted(address: String): Single<BlockingClient.Action> =
        Single.fromCallable {
                when (blockingRepo.isBlocked(address)) {
                    true -> BlockingClient.Action.Block()
                    false -> BlockingClient.Action.Unblock
                }
            }
            .subscribeOn(Schedulers.io())

    override fun block(addresses: List<String>): Completable =
        Completable.fromCallable { blockingRepo.blockNumber(*addresses.toTypedArray()) }
            .subscribeOn(Schedulers.io())

    override fun unblock(addresses: List<String>): Completable =
        Completable.fromCallable { blockingRepo.unblockNumbers(*addresses.toTypedArray()) }
            .subscribeOn(Schedulers.io())

    override fun openSettings() = Unit // TODO: Do this here once we implement AndroidX navigation
}
