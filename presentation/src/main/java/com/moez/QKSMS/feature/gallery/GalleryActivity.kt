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

package org.prauga.messages.feature.gallery

import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dagger.android.AndroidInjection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import org.prauga.messages.R
import org.prauga.messages.common.base.QkActivity
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.databinding.GalleryActivityBinding
import org.prauga.messages.model.MmsPart
import javax.inject.Inject

class GalleryActivity :
    QkActivity<GalleryActivityBinding>(GalleryActivityBinding::inflate),
    GalleryView {

    @Inject
    lateinit var dateFormatter: DateFormatter

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var pagerAdapter: GalleryPagerAdapter

    val partId by lazy { intent.getLongExtra("partId", 0L) }

    private val _optionsItemSelected = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val _pageChanged = MutableSharedFlow<MmsPart>(extraBufferCapacity = 1)
    private val _screenTouched = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val viewModel by lazy {
        ViewModelProviders.of(
            this,
            viewModelFactory
        )[GalleryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gallery_activity)
        showBackButton(true)
        viewModel.bindView(this)

        binding.pager.adapter = pagerAdapter
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                this@GalleryActivity.onPageSelected(position)
            }
        })

        pagerAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                pagerAdapter.data?.takeIf { pagerAdapter.itemCount > 0 }
                    ?.indexOfFirst { part -> part.id == partId }
                    ?.let { index ->
                        onPageSelected(index)
                        binding.pager.setCurrentItem(index, false)
                        pagerAdapter.unregisterAdapterDataObserver(this)
                    }
            }
        })

        lifecycleScope.launch {
            pagerAdapter.clicks
                .asFlow()
                .collect {
                    _screenTouched.emit(Unit)
                }
        }
    }

    fun onPageSelected(position: Int) {
        val part = pagerAdapter.getItem(position)

        binding.toolbarSubtitle.text = pagerAdapter.getItem(position)?.messages?.firstOrNull()?.date
            ?.let(dateFormatter::getDetailedTimestamp)
        binding.toolbarSubtitle.isVisible = binding.toolbarTitle.text.isNotBlank()

        if (part != null) {
            lifecycleScope.launch {
                _pageChanged.emit(part)
            }
        }
    }

    override fun render(state: GalleryState) {
        binding.toolbar.setVisible(state.navigationVisible)

        title = state.title
        pagerAdapter.updateData(state.parts)
    }

    override fun optionsItemSelected(): Flow<Int> = _optionsItemSelected
    override fun screenTouched(): Flow<Unit> = _screenTouched
    override fun pageChanged(): Flow<MmsPart> = _pageChanged

    override fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            0
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mms_part_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> lifecycleScope.launch {
                _optionsItemSelected.emit(item.itemId)
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        pagerAdapter.destroy()
    }
}