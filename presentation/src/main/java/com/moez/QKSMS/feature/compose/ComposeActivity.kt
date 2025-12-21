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
package org.prauga.messages.feature.compose

import android.Manifest
import android.animation.LayoutTransition
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.ContextMenu
import android.view.DragEvent.ACTION_DRAG_ENDED
import android.view.DragEvent.ACTION_DRAG_EXITED
import android.view.DragEvent.ACTION_DROP
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.moez.QKSMS.common.QkMediaPlayer
import com.uber.autodispose.ObservableSubscribeProxy
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dagger.android.AndroidInjection
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkThemedActivity
import org.prauga.messages.common.util.DateFormatter
import org.prauga.messages.common.util.extensions.autoScrollToStart
import org.prauga.messages.common.util.extensions.dpToPx
import org.prauga.messages.common.util.extensions.hideKeyboard
import org.prauga.messages.common.util.extensions.makeToast
import org.prauga.messages.common.util.extensions.scrapViews
import org.prauga.messages.common.util.extensions.setBackgroundTint
import org.prauga.messages.common.util.extensions.setTint
import org.prauga.messages.common.util.extensions.setVisible
import org.prauga.messages.common.util.extensions.showKeyboard
import org.prauga.messages.common.util.extensions.resolveThemeColor
import org.prauga.messages.common.widget.MicInputCloudView
import org.prauga.messages.databinding.ComposeActivityBinding
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.feature.compose.editing.ChipsAdapter
import org.prauga.messages.feature.contacts.ContactsActivity
import org.prauga.messages.model.Attachment
import org.prauga.messages.model.Recipient
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ComposeActivity : QkThemedActivity<ComposeActivityBinding>(ComposeActivityBinding::inflate), ComposeView {

    @Inject lateinit var composeAttachmentAdapter: ComposeAttachmentAdapter
    @Inject lateinit var chipsAdapter: ChipsAdapter
    @Inject lateinit var dateFormatter: DateFormatter
    @Inject lateinit var messageAdapter: MessagesAdapter
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override val activityVisibleIntent: Subject<Boolean> = PublishSubject.create()
    override val chipsSelectedIntent: Subject<HashMap<String, String?>> = PublishSubject.create()
    override val chipDeletedIntent: Subject<Recipient> by lazy { chipsAdapter.chipDeleted }
    override val menuReadyIntent: Observable<Unit> = menu.map { Unit }
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val contextItemIntent: Subject<MenuItem> = PublishSubject.create()
    override val scheduleAction: Subject<Boolean> = PublishSubject.create()
    override val sendAsGroupIntent by lazy { binding.sendAsGroupBackground.clicks() }
    override val messagePartClickIntent: Subject<Long> by lazy { messageAdapter.partClicks }
    override val messagePartContextMenuRegistrar: Subject<View> by lazy { messageAdapter.partContextMenuRegistrar }
    override val messagesSelectedIntent by lazy { messageAdapter.selectionChanges }
    override val cancelSendingIntent: Subject<Long> by lazy { messageAdapter.cancelSendingClicks }
    override val sendNowIntent: Subject<Long> by lazy { messageAdapter.sendNowClicks }
    override val resendIntent: Subject<Long> by lazy { messageAdapter.resendClicks }
    override val attachmentDeletedIntent: Subject<Attachment> by lazy { composeAttachmentAdapter.attachmentDeleted }
    override val textChangedIntent by lazy { binding.message.textChanges() }
    override val attachIntent by lazy { Observable.merge(binding.attach.clicks(), binding.shadeBackground.clicks()) }
    override val cameraIntent by lazy { Observable.merge(binding.camera.clicks(), binding.cameraLabel.clicks()) }
    override val attachImageFileIntent by lazy { Observable.merge(binding.gallery.clicks(), binding.galleryLabel.clicks()) }
    override val attachAnyFileIntent by lazy { Observable.merge(binding.attachAFileIcon.clicks(), binding.attachAFileLabel.clicks()) }
    override val scheduleIntent by lazy { Observable.merge(binding.schedule.clicks(), binding.scheduleLabel.clicks()) }
    override val attachContactIntent by lazy { Observable.merge(binding.contact.clicks(), binding.contactLabel.clicks()) }
    override val attachAnyFileSelectedIntent: Subject<Uri> = PublishSubject.create()
    override val contactSelectedIntent: Subject<Uri> = PublishSubject.create()
    override val inputContentIntent by lazy { binding.message.inputContentSelected }
    override val scheduleSelectedIntent: Subject<Long> = PublishSubject.create()
    override val changeSimIntent by lazy { binding.sim.clicks() }
    override val scheduleCancelIntent by lazy { binding.scheduledCancel.clicks() }
    override val sendIntent by lazy {  Observable.merge(binding.send.clicks(), binding.scheduledSend.clicks()) }
    override val viewQksmsPlusIntent: Subject<Unit> = PublishSubject.create()
    override val backPressedIntent: Subject<Unit> = PublishSubject.create()
    override val confirmDeleteIntent: Subject<List<Long>> = PublishSubject.create()
    override val clearCurrentMessageIntent: Subject<Boolean> = PublishSubject.create()
    override val messageLinkAskIntent: Subject<Uri> by lazy { messageAdapter.messageLinkClicks }
    override val shadeIntent by lazy { binding.shadeBackground.clicks() }
    override val recordAudioStartStopRecording: Subject<Boolean> = PublishSubject.create()
    override val recordAnAudioMessage by lazy {
        Observable.merge(binding.recordAudioMsg.clicks(),
            binding.attachAnAudioMessageIcon.clicks(),
            binding.attachAnAudioMessageLabel.clicks())
    }
    override val recordAudioAbort by lazy { binding.audioMsgAbort.clicks() }
    override val recordAudioAttach by lazy { binding.audioMsgAttach.clicks() }
    override val recordAudioPlayerPlayPause: Subject<QkMediaPlayer.PlayingState> = PublishSubject.create()
    override val recordAudioPlayerConfigUI: Subject<QkMediaPlayer.PlayingState> = PublishSubject.create()
    override val recordAudioPlayerVisible: Subject<Boolean> = PublishSubject.create()
    override val recordAudioMsgRecordVisible: Subject<Boolean> = PublishSubject.create()
    override val recordAudioChronometer: Subject<Boolean> = PublishSubject.create()
    override val recordAudioRecord: Subject<MicInputCloudView.ViewState> = PublishSubject.create()

    private var seekBarUpdater: Disposable? = null

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[ComposeViewModel::class.java] }

    private var cameraDestination: Uri? = null
    private var isSelection = false

    private fun getSeekBarUpdater(): ObservableSubscribeProxy<Long> {
        return Observable.interval(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(scope())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        showBackButton(true)
        viewModel.bindView(this)

        binding.contentView.layoutTransition = LayoutTransition().apply {
            disableTransitionType(LayoutTransition.CHANGING)
        }
            chipsAdapter.view = binding.chips

            binding.chips.itemAnimator = null
            binding.chips.layoutManager = FlexboxLayoutManager(this)

            messageAdapter.autoScrollToStart(binding.messageList)
            messageAdapter.emptyView = binding.messagesEmpty

            binding.messageList.setHasFixedSize(true)
            binding.messageList.adapter = messageAdapter

            binding.messageAttachments.adapter = composeAttachmentAdapter

            binding.message.supportsInputContent = true

            theme
                .doOnNext {
                    binding.loading.setTint(it.theme)

                    // entire attach menu
                    binding.attach.setBackgroundTint(it.theme); binding.attach.setTint(it.textPrimary)
                    binding.contact.setBackgroundTint(it.theme); binding.contact.setTint(it.textPrimary)
                    binding.contactLabel.setBackgroundTint(it.theme); binding.contactLabel.setTint(it.textPrimary)
                    binding.schedule.setBackgroundTint(it.theme); binding.schedule.setTint(it.textPrimary)
                    binding.scheduleLabel.setBackgroundTint(it.theme); binding.scheduleLabel.setTint(it.textPrimary)
                    binding.attachAFileIcon.setBackgroundTint(it.theme); binding.attachAFileIcon.setTint(it.textPrimary)
                    binding.attachAFileLabel.setBackgroundTint(it.theme); binding.attachAFileLabel.setTint(it.textPrimary)
                    binding.attachAnAudioMessageIcon.setBackgroundTint(it.theme); binding.attachAnAudioMessageIcon.setTint(it.textPrimary)
                    binding.attachAnAudioMessageLabel.setBackgroundTint(it.theme); binding.attachAnAudioMessageLabel.setTint(it.textPrimary)
                    binding.gallery.setBackgroundTint(it.theme); binding.gallery.setTint(it.textPrimary)
                    binding.galleryLabel.setBackgroundTint(it.theme); binding.galleryLabel.setTint(it.textPrimary)
                    binding.camera.setBackgroundTint(it.theme); binding.camera.setTint(it.textPrimary)
                    binding.cameraLabel.setBackgroundTint(it.theme); binding.cameraLabel.setTint(it.textPrimary)

                    // audio message recording
                    binding.audioMsgRecord.setColor(it.theme)
                    binding.audioMsgPlayerPlayPause.setTint(it.theme)
                    binding.audioMsgPlayerSeekBar.apply {
                        thumbTintList = ColorStateList.valueOf(it.theme)
                        progressBackgroundTintList = ColorStateList.valueOf(it.theme)
                        progressTintList = ColorStateList.valueOf(it.theme)
                    }

                    messageAdapter.theme = it
                }
                .autoDisposable(scope())
                .subscribe()

            // context menu registration for message parts
            messagePartContextMenuRegistrar
                .mapNotNull { it }
                .autoDisposable(scope())
                .subscribe { registerForContextMenu(it) }

            // start/stop audio message recording
            binding.audioMsgRecord.setOnClickListener {
                recordAudioRecord.onNext(binding.audioMsgRecord.getState())
            }

            recordAudioChronometer
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .autoDisposable(scope())
                .subscribe {
                    if (it) {
                        binding.audioMsgDuration.base = SystemClock.elapsedRealtime()
                        binding.audioMsgDuration.start()
                    } else {
                        binding.audioMsgDuration.stop()
                    }
                }

            // audio record playback play/pause button
            binding.audioMsgPlayerPlayPause.setOnClickListener {
                recordAudioPlayerPlayPause.onNext(
                    binding.audioMsgPlayerPlayPause.tag as QkMediaPlayer.PlayingState
                )
            }

            recordAudioMsgRecordVisible
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .autoDisposable(scope())
                .subscribe {
                    binding.audioMsgRecord.isVisible = it
                    binding.audioMsgDuration.isVisible =
                        it   // chronometer follows record button visibility
                    binding.audioMsgBluetooth.isVisible = !it
                }

            recordAudioPlayerVisible
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .autoDisposable(scope())
                .subscribe {
                    binding.audioMsgPlayerBackground.isVisible = it
                    recordAudioPlayerConfigUI.onNext(QkMediaPlayer.PlayingState.Stopped)
                }

            recordAudioPlayerConfigUI
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .autoDisposable(scope())
                .subscribe {
                    when (it) {
                        QkMediaPlayer.PlayingState.Playing -> {
                            binding.audioMsgPlayerPlayPause.tag = QkMediaPlayer.PlayingState.Playing
                            QkMediaPlayer.start()
                            binding.audioMsgPlayerPlayPause.setImageResource(R.drawable.exo_icon_pause)
                            seekBarUpdater = getSeekBarUpdater().subscribe {
                                binding.audioMsgPlayerSeekBar.progress = QkMediaPlayer.currentPosition
                                binding.audioMsgPlayerSeekBar.max = QkMediaPlayer.duration
                            }
                            binding.audioMsgPlayerSeekBar.isEnabled = true
                        }

                        QkMediaPlayer.PlayingState.Paused -> {
                            binding.audioMsgPlayerPlayPause.tag = QkMediaPlayer.PlayingState.Paused
                            QkMediaPlayer.pause()
                            binding.audioMsgPlayerPlayPause.setImageResource(R.drawable.exo_icon_play)
                            seekBarUpdater?.dispose()
                        }

                        else -> {
                            binding.audioMsgPlayerPlayPause.tag = QkMediaPlayer.PlayingState.Stopped
                            QkMediaPlayer.reset()
                            binding.audioMsgPlayerPlayPause.setImageResource(R.drawable.exo_icon_play)
                            seekBarUpdater?.dispose()
                            binding.audioMsgPlayerSeekBar.progress = 0
                            binding.audioMsgPlayerSeekBar.isEnabled = false
                        }
                    }
                }
            // audio msg player seek bar handler
            binding.audioMsgPlayerSeekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                        // if seek was initiated by the user and this part is currently playing
                        if (fromUser)
                            QkMediaPlayer.seekTo(progress)
                    }
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                }
            )

            window.callback = ComposeWindowCallback(window.callback, this)
    }

    override fun onStart() {
        super.onStart()
        activityVisibleIntent.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        activityVisibleIntent.onNext(false)
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop any playing audio
        QkMediaPlayer.reset()

        seekBarUpdater?.dispose()
    }


    override fun render(state: ComposeState) {
        if (state.hasError) {
            finish()
            return
        }

        threadId.onNext(state.threadId)

        title = when {
            state.selectedMessages > 0 -> getString(R.string.compose_title_selected, state.selectedMessages)
            state.query.isNotEmpty() -> state.query
            else -> state.conversationtitle
        }

        binding.toolbarSubtitle.setVisible(state.query.isNotEmpty())
        binding.toolbarSubtitle.text = getString(R.string.compose_subtitle_results, state.searchSelectionPosition,
            state.searchResults)

        binding.toolbarTitle.setVisible(!state.editingMode)
        binding.chips.setVisible(state.editingMode)
        binding.composeBar.setVisible(!state.loading)

        // Don't set the adapters unless needed
        if (state.editingMode && binding.chips.adapter == null) binding.chips.adapter = chipsAdapter

        binding.toolbar.menu.findItem(R.id.viewScheduledMessages)?.isVisible = !state.editingMode && state.selectedMessages == 0
                && state.query.isEmpty() && state.hasScheduledMessages
        binding.toolbar.menu.findItem(R.id.select_all)?.isVisible = !state.editingMode && (messageAdapter.itemCount > 1) && state.selectedMessages != 0
        binding.toolbar.menu.findItem(R.id.add)?.isVisible = state.editingMode
        binding.toolbar.menu.findItem(R.id.call)?.isVisible = !state.editingMode && state.selectedMessages == 0
                && state.query.isEmpty()
        binding.toolbar.menu.findItem(R.id.info)?.isVisible = !state.editingMode && state.selectedMessages == 0
                && state.query.isEmpty()
        binding.toolbar.menu.findItem(R.id.copy)?.isVisible =
            !state.editingMode && state.selectedMessages > 0 && state.selectedMessagesHaveText
        binding.toolbar.menu.findItem(R.id.share)?.isVisible =
            !state.editingMode && state.selectedMessages > 0 && state.selectedMessagesHaveText
        binding.toolbar.menu.findItem(R.id.details)?.isVisible = !state.editingMode && state.selectedMessages == 1
        binding.toolbar.menu.findItem(R.id.delete)?.isVisible = !state.editingMode && ((state.selectedMessages > 0) || state.canSend)
        binding.toolbar.menu.findItem(R.id.forward)?.isVisible = !state.editingMode && state.selectedMessages == 1
        binding.toolbar.menu.findItem(R.id.show_status)?.isVisible = !state.editingMode && state.selectedMessages > 0
        binding.toolbar.menu.findItem(R.id.previous)?.isVisible = state.selectedMessages == 0 && state.query.isNotEmpty()
        binding.toolbar.menu.findItem(R.id.next)?.isVisible = state.selectedMessages == 0 && state.query.isNotEmpty()
        binding.toolbar.menu.findItem(R.id.clear)?.isVisible = state.selectedMessages == 0 && state.query.isNotEmpty()

        chipsAdapter.data = state.selectedChips

        binding.loading.setVisible(state.loading)

        binding.sendAsGroup.setVisible(state.editingMode && state.selectedChips.size >= 2)
        binding.sendAsGroupSwitch.isChecked = state.sendAsGroup

        binding.messageList.setVisible(!state.editingMode || state.sendAsGroup || state.selectedChips.size == 1)
        messageAdapter.data = state.messages
        messageAdapter.highlight = state.searchSelectionId

        binding.scheduledGroup.isVisible = state.scheduled != 0L
        binding.scheduledTime.text = dateFormatter.getScheduledTimestamp(state.scheduled)

        binding.messageAttachments.setVisible(state.attachments.isNotEmpty())
        composeAttachmentAdapter.data = state.attachments

        binding.attach.animate().rotation(if (state.attaching) 135f else 0f).start()
        binding.attaching.isVisible = state.attaching

        binding.shadeBackground.apply {
            when {
                state.attaching -> {
                    visibility = View.VISIBLE
                    elevation = 4.dpToPx(context).toFloat() // below attach menu
                }

                state.audioMsgRecording -> {
                    visibility = View.VISIBLE
                    elevation = 5.dpToPx(context).toFloat() // above attach menu
                }

                else-> visibility = View.GONE
            }
        }

        // show or hide audio message recording panel and shade background
        binding.audioMsgBackground.isVisible = state.audioMsgRecording

        binding.counter.text = state.remaining
        binding.counter.setVisible(binding.counter.text.isNotBlank())

        binding.sim.setVisible(state.subscription != null)
        binding.sim.contentDescription = getString(R.string.compose_sim_cd, state.subscription?.displayName)
        binding.simIndex.text = state.subscription?.simSlotIndex?.plus(1)?.toString()

        // show either send, audio msg record, or sendScheduled button
        binding.send.visibility = if (state.canSend && !state.loading && state.scheduled == 0L) View.VISIBLE else View.INVISIBLE
        binding.recordAudioMsg.visibility = if (state.canSend && !state.loading) View.INVISIBLE else View.VISIBLE
        binding.scheduledSend.visibility = if (state.canSend && (state.scheduled != 0L) && !state.loading) View.VISIBLE else View.INVISIBLE

        // if not in editing mode, and there are no non-me participants that can be sent to,
        // hide controls that allow constructing a reply and inform user no valid recipients
        if (!state.editingMode && (state.validRecipientNumbers == 0)) {
            binding.composeBar.visibility = View.GONE
            binding.sim.visibility = View.GONE
            binding.recordAudioMsg.visibility = View.GONE
            binding.noValidRecipients.visibility = View.VISIBLE

            // change constraint of messageList to constrain bottom to top of noValidRecipients
            ConstraintSet().apply {
                clone(binding.contentView)
                connect(
                    R.id.messageList,
                    ConstraintSet.BOTTOM,
                    R.id.noValidRecipients,
                    ConstraintSet.TOP,
                    0
                )
                applyTo(binding.contentView)
            }
        }

        // if scheduling mode is set, show schedule dialog
        if (state.scheduling)
            scheduleAction.onNext(true)

    }

    override fun clearSelection() = messageAdapter.clearSelection()

    override fun toggleSelectAll() {
        messageAdapter.toggleSelectAll()
    }

    override fun expandMessages(messageIds: List<Long>, expand: Boolean) {
        messageAdapter.expandMessages(messageIds, expand)
    }

    override fun showDetails(details: String) {
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.compose_details_title)
            .setMessage(details)
            .setCancelable(true)
            .create()

        dialog.show()

        theme.take(1)
            .autoDisposable(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun showMessageLinkAskDialog(uri: Uri) {
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.messageLinkHandling_dialog_title)
            .setMessage(getString(R.string.messageLinkHandling_dialog_body, uri.toString()))
            .setPositiveButton(
                R.string.messageLinkHandling_dialog_positive
            ) { _, _ ->
                ContextCompat.startActivity(
                    this,
                    Intent(Intent.ACTION_VIEW).setData(uri),
                    null
                )
            }
            .setNegativeButton(R.string.messageLinkHandling_dialog_negative) { _, _ -> { } }
            .create()

        dialog.show()

        theme.take(1)
            .autoDisposable(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun requestDefaultSms() {
        navigator.showDefaultSmsDialog(this)
    }

    override fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
    }

    override fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
    }

    override fun requestSmsPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS), 0)
    }

    override fun requestDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val timePicker = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                scheduleSelectedIntent.onNext(calendar.timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(this))
            tintDialogButtons(timePicker)
            timePicker.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        tintDialogButtons(datePicker)
        datePicker.show()

        // On some devices, the keyboard can cover the date picker
        binding.message.hideKeyboard()
    }

    override fun requestContact() {
        val intent = Intent(Intent.ACTION_PICK)
            .setType(ContactsContract.Contacts.CONTENT_TYPE)

        startActivityForResult(Intent.createChooser(intent, null), ComposeView.AttachContactRequestCode)
    }

    override fun showContacts(sharing: Boolean, chips: List<Recipient>) {
        binding.message.hideKeyboard()
        // Track if this is the initial contact selection (no existing chips)
        isSelection = chips.isEmpty()
        val serialized = HashMap(chips.associate { chip -> chip.address to chip.contact?.lookupKey })
        val intent = Intent(this, ContactsActivity::class.java)
            .putExtra(ContactsActivity.SharingKey, sharing)
            .putExtra(ContactsActivity.ChipsKey, serialized)
        startActivityForResult(intent, ComposeView.SelectContactRequestCode)
    }

    override fun themeChanged() {
        binding.messageList.scrapViews()
    }

    override fun showKeyboard() {
        binding.message.postDelayed({
            binding.message.showKeyboard()
        }, 200)
    }

    override fun requestCamera() {
        cameraDestination = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            .let { timestamp -> ContentValues().apply { put(MediaStore.Images.Media.TITLE, timestamp) } }
            .let { cv -> contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv) }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, cameraDestination)
        startActivityForResult(Intent.createChooser(intent, null), ComposeView.TakePhotoRequestCode)
    }

    override fun requestGallery(mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            .putExtra(Intent.EXTRA_LOCAL_ONLY, false)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setType(mimeType)
        startActivityForResult(Intent.createChooser(intent, null), requestCode)
    }

    override fun setDraft(draft: String) {
        binding.message.setText(draft)
        binding.message.setSelection(draft.length)
    }

    override fun scrollToMessage(id: Long) {
        messageAdapter.data?.second
            ?.indexOfLast { message -> message.id == id }
            ?.takeIf { position -> position != -1 }
            ?.let(binding.messageList::scrollToPosition)
    }

    override fun showQksmsPlusSnackbar(message: Int) {
        Snackbar.make(binding.contentView, message, Snackbar.LENGTH_LONG).run {
            setAction(R.string.button_more) { viewQksmsPlusIntent.onNext(Unit) }
            setActionTextColor(colors.theme().theme)
            show()
        }
    }

    override fun showDeleteDialog(messages: List<Long>) {
        val count = messages.size
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(resources.getQuantityString(R.plurals.dialog_delete_chat, count, count))
            .setPositiveButton(R.string.button_delete) { _, _ -> confirmDeleteIntent.onNext(messages) }
            .setNegativeButton(R.string.button_cancel, null)
            .create()

        dialog.show()

        theme.take(1)
            .autoDisposable(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun showClearCurrentMessageDialog() {
        val dialog = AlertDialog.Builder(this, R.style.AppThemeDialog)
            .setTitle(R.string.dialog_clear_compose_title)
            .setMessage(R.string.dialog_clear_compose)
            .setPositiveButton(R.string.button_clear) { _, _ ->
                clearCurrentMessageIntent.onNext(false)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()

        dialog.show()

        theme.take(1)
            .autoDisposable(scope())
            .subscribe { theme ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(theme.theme)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(theme.theme)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.compose, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        return true
    }

    override fun getColoredMenuItems(): List<Int> {
        return super.getColoredMenuItems() + R.id.call
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.mms_part_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        super.onContextItemSelected(item)
        contextItemIntent.onNext(item)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK)
            return

        when (requestCode) {
            ComposeView.SelectContactRequestCode -> {
                val selectedContacts = data?.getSerializableExtra(ContactsActivity.ChipsKey)
                    ?.let { serializable -> serializable as? HashMap<String, String?> }
                    ?: hashMapOf()

                // finish immediately to avoid showing empty compose screen
                if (isSelection && selectedContacts.isEmpty()) {
                    finish()
                    return
                }

                // Reset the flag after handling
                isSelection = false
                chipsSelectedIntent.onNext(selectedContacts)
            }

            ComposeView.TakePhotoRequestCode -> {
                cameraDestination?.let(attachAnyFileSelectedIntent::onNext)
            }

            ComposeView.AttachAFileRequestCode -> {
                data?.clipData?.itemCount
                    ?.let { count -> 0 until count }
                    ?.mapNotNull { i -> data.clipData?.getItemAt(i)?.uri }
                    ?.forEach(attachAnyFileSelectedIntent::onNext)
                    ?: data?.data?.let(attachAnyFileSelectedIntent::onNext)
            }

            ComposeView.AttachContactRequestCode -> {
                data?.data?.let(contactSelectedIntent::onNext)
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(ComposeView.CameraDestinationKey, cameraDestination)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        cameraDestination = savedInstanceState.getParcelable(ComposeView.CameraDestinationKey)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onBackPressed() = backPressedIntent.onNext(Unit)

    override fun focusMessage() {
        binding.message.requestFocus()
    }

    private fun tintDialogButtons(dialog: android.app.AlertDialog) {
        dialog.setOnShowListener {
            val color = resolveThemeColor(android.R.attr.textColorPrimary, colors.theme().textPrimary)
            listOf(
                DialogInterface.BUTTON_POSITIVE,
                DialogInterface.BUTTON_NEGATIVE,
                DialogInterface.BUTTON_NEUTRAL
            ).forEach { type ->
                dialog.getButton(type)?.setTextColor(color)
            }
        }
    }
}
