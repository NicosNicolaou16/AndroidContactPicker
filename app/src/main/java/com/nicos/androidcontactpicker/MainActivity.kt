package com.nicos.androidcontactpicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsPickerSessionContract.ACTION_PICK_CONTACTS
import android.provider.ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS
import android.provider.ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS
import android.provider.ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nicos.androidcontactpicker.data.Contact
import com.nicos.androidcontactpicker.ui.theme.AndroidContactPickerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidContactPickerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContactPicker(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private suspend fun processContactPickerResultUri(
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

@Composable
fun ContactPicker(modifier: Modifier = Modifier) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickContact =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val resultUri = it.data?.data ?: return@rememberLauncherForActivityResult

                // Process the result URI in a background thread
                scope.launch {
                    contacts = processContactPickerResultUri(resultUri, context)
                    Log.d("ContactInfo", contacts[0].name)
                }
            }
        }

    val requestedFields = arrayListOf(
        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
    )

    val pickContactIntent = Intent(ACTION_PICK_CONTACTS).apply {
        putExtra(EXTRA_PICK_CONTACTS_SELECTION_LIMIT, 5)
        putStringArrayListExtra(
            EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
            requestedFields
        )
        putExtra(EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS, false)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Button(onClick = {
            pickContact.launch(pickContactIntent)
        }) {
            androidx.compose.material3.Text(text = "Pick Contacts")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidContactPickerTheme {
        ContactPicker()
    }
}