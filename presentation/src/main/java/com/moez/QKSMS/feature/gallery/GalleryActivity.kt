/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.gallery

import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkActivity
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.databinding.GalleryActivityBinding
import org.prauga.messages.model.MmsPart

class GalleryActivity :
    QkActivity<GalleryActivityBinding>(GalleryActivityBinding::inflate), GalleryView {

    @Inject lateinit var dateFormatter: DateFormatter

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject lateinit var pagerAdapter: GalleryPagerAdapter

    val partId by lazy { intent.getLongExtra("partId", 0L) }

    private val optionsItemSubject: Subject<Int> = PublishSubject.create()
    private val pageChangedSubject: Subject<MmsPart> = PublishSubject.create()
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[GalleryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        showBackButton(true)
        viewModel.bindView(this)

        binding.pager.adapter = pagerAdapter
        binding.pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    this@GalleryActivity.onPageSelected(position)
                }
            }
        )

        pagerAdapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    pagerAdapter.data
                        ?.takeIf { pagerAdapter.itemCount > 0 }
                        ?.indexOfFirst { part -> part.id == partId }
                        ?.let { index ->
                            onPageSelected(index)
                            binding.pager.setCurrentItem(index, false)
                            pagerAdapter.unregisterAdapterDataObserver(this)
                        }
                }
            }
        )
    }

    fun onPageSelected(position: Int) {
        binding.toolbarSubtitle.text =
            pagerAdapter
                .getItem(position)
                ?.messages
                ?.firstOrNull()
                ?.date
                ?.let(dateFormatter::getDetailedTimestamp)
        binding.toolbarSubtitle.isVisible = binding.toolbarTitle.text.isNotBlank()

        pagerAdapter.getItem(position)?.run(pageChangedSubject::onNext)
    }

    override fun render(state: GalleryState) {
        binding.toolbar.setVisible(state.navigationVisible)

        title = state.title
        pagerAdapter.updateData(state.parts)
    }

    override fun optionsItemSelected(): Observable<Int> = optionsItemSubject

    override fun screenTouched(): Observable<*> = pagerAdapter.clicks

    override fun pageChanged(): Observable<MmsPart> = pageChangedSubject

    override fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            0,
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mms_part_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> optionsItemSubject.onNext(item.itemId)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        pagerAdapter.destroy()
    }
}
