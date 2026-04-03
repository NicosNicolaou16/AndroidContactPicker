package com.nicos.androidcontactpicker.contact_picker

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.nicos.androidcontactpicker.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun processContactPickerResultUri(
    sessionUri: Uri,
    context: Context
): List<Contact> = withContext(Dispatchers.IO) {
    // Define the columns we want to retrieve from the ContactPicker ContentProvider
    val projection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        ContactsContract.Data.MIMETYPE, // Type of data (e.g., email or phone)
        ContactsContract.Data.DATA1, // The actual data (Phone number / Email string)
    )

    val results = mutableListOf<Contact>()

    // Note: The Contact Picker Session Uri doesn't support custom selection & selectionArgs.
    context.contentResolver.query(sessionUri, projection, null, null, null)?.use { cursor ->
        // Get the column indices for our requested projection
        val contactIdIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val mimeTypeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
        val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)

        while (cursor.moveToNext()) {
            val contactId = cursor.getString(contactIdIdx)
            val mimeType = cursor.getString(mimeTypeIdx)
            val name = cursor.getString(nameIdx) ?: ""
            val data1 = cursor.getString(data1Idx) ?: ""

            // Determine if the current row represents an email or a phone number
            val email =
                if (mimeType == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) data1 else null
            val phone =
                if (mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) data1 else null

            // Add the parsed contact to our results list
            results.add(Contact(contactId, name, email, phone))
        }
    }

    return@withContext results
}