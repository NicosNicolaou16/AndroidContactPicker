package com.nicos.androidcontactpicker

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.nicos.androidcontactpicker.contact_picker.processContactPickerResultUri
import com.nicos.androidcontactpicker.data.Contact
import com.nicos.androidcontactpicker.ui.theme.AndroidContactPickerTheme
import kotlinx.coroutines.launch

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

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Scrollable list of contacts (takes up space above the button if contacts exist)
        if (contacts.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Takes available space
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(contacts) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = contact.phone ?: "No Phone", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            // Spacer to keep the button centered when list is empty
            Spacer(modifier = Modifier.weight(1f))
        }

        // Button remains centered (at the bottom of the list or middle of screen)
        Button(
            onClick = { pickContact.launch(pickContactIntent) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Pick Contacts")
        }

        if (contacts.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
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