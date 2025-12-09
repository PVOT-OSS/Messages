package org.prauga.messages.util

import android.net.Uri
import org.prauga.messages.util.Preference.Serializer

class UriPreferenceConverter : Serializer<Uri> {

    override fun deserialize(serialized: String): Uri {
        return Uri.parse(serialized)
    }

    override fun serialize(value: Uri): String {
        return value.toString()
    }

}
