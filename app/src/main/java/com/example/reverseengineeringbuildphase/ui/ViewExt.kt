package com.example.reverseengineeringbuildphase.ui

import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText

fun TextInputEditText.doAfterTextChangedCompat(onTextChanged: (String) -> Unit) {
    addTextChangedListener {
        onTextChanged(it?.toString().orEmpty())
    }
}
