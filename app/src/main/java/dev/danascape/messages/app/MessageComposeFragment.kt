package dev.danascape.messages.app

import android.app.Dialog
import android.os.Bundle
import android.view.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.widget.ImageButton
import com.google.android.material.textfield.TextInputEditText

class MessageComposeFragment : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { d ->
            val bottomSheet = (d as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_message_compose, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val closeBtn = view.findViewById<ImageButton>(R.id.close_btn)
        closeBtn.setOnClickListener { dismiss() }

        val toPlus = view.findViewById<ImageButton>(R.id.to_plus_btn)
        toPlus.setOnClickListener {
            // hook to open contacts or add recipient
        }

        val leftPlus = view.findViewById<ImageButton>(R.id.left_plus)
        leftPlus.setOnClickListener {
            // attach media
        }

        val voiceBtn = view.findViewById<ImageButton>(R.id.voice_btn)
        voiceBtn.setOnClickListener {
            // start recording
        }

        val messageEdit = view.findViewById<TextInputEditText>(R.id.message_edit_text)
        messageEdit.requestFocus()
        // Show soft keyboard automatically
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
}