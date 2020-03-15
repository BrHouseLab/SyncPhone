/*
Author: Найденов Антон BrHouse Lab.
email: brhouselab@gmail.com

Лицензия:
- Данное ПО можно свободно использовать для личных целей. Для коммерческих целей
необходимо получения лицензионного соглашения.
*/

package by.brhouselab.syncphone


import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.ContactsContract.*
import android.provider.ContactsContract.CommonDataKinds.*
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 9682
    private val SYNC_ID = "MILETODA_"
    private val handler = Handler {
        progressBar.apply {
            max = it.arg2
            progress = it.arg1
        }
        return@Handler true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prepareElements()
    }

    private fun prepareElements() {
        val pref = getPreferences(Context.MODE_PRIVATE)

        execForOneElms { it.setText(pref.getString(it.tag.toString(), "")) }
    }

    private fun execForOneElms(body: (EditText) -> Unit) {
        listOf(R.id.HostField, R.id.LoginField, R.id.PassField, R.id.tableField)
            .map {
                findViewById<EditText>(it)
            }
            .forEach {
                body(it)
            }
    }

    fun onClickHandler(view: View) {
        //Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            == PackageManager.PERMISSION_GRANTED
        ) {
            executeSync()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.INTERNET
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.size >= 2) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                executeSync()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun executeSync() {
        //Save DB params in Preferences
        val pref = getPreferences(Context.MODE_PRIVATE).edit()
        execForOneElms { pref.putString(it.tag.toString(), it.text.toString()) }
        pref.apply()

        val host = findViewById<EditText>(R.id.HostField).text.toString()
        val login = findViewById<EditText>(R.id.LoginField).text.toString()
        val pass = findViewById<EditText>(R.id.PassField).text.toString()
        val table = findViewById<EditText>(R.id.tableField).text.toString()


        val task = SQLHandler(table, handler).apply {
            execute(host, login, pass)
        }

        AsyncTask.execute {
            val result = task.get()

            runOnUiThread {
                handler.sendMessage(Message().apply { arg1 = 0; arg2 = 0 })
                Toast.makeText(
                    applicationContext,
                    task.resultMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }

            if (result.isEmpty())
                return@execute

            var messageForUpd = "Синхронизация завершена"
            try {
                updateOrInsertContacts(result)
            } catch (e: Exception) {
                messageForUpd = "Синхронизация отклонена системой"
            }
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    messageForUpd,
                    Toast.LENGTH_SHORT
                ).show()
            }
            handler.sendMessage(Message().apply { arg1 = 0; arg2 = 0 })
        }


    }

    private fun updateOrInsertContacts(contacts: List<Contact>) {
        try {
            clearContactsBeforeInsert()
            contacts.forEachIndexed { index, contact ->
                insertContact(contact)
                handler.sendMessage(Message().apply { arg1 = index; arg2 = contacts.size })
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Процесс обновления контактов отклонен телефоном",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun insertContact(contact: Contact) {

        val ops = ArrayList<ContentProviderOperation>()

        ops.apply {
            add(
                ContentProviderOperation
                    .newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_NAME, null)
                    .withValue(RawContacts.ACCOUNT_TYPE, null)
                    .withValue(RawContacts.SYNC1, SYNC_ID + contact.ppl_id)
                    .build()
            )
            add(
                contPrvOpsActionInsertWithType(StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.GIVEN_NAME, contact.ppl_name)
                    .withValue(StructuredName.FAMILY_NAME, contact.ppl_surn)
                    .withValue(StructuredName.MIDDLE_NAME, contact.ppl_secd)
                    .withValue(StructuredName.SUFFIX, contact.ppl_maiden)
                    .build()
            )
            if (contact.ppl_birth.time != 0L) {
                add(
                    contPrvOpsActionInsertWithType(Event.CONTENT_ITEM_TYPE)
                        .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
                        .withValue(Event.START_DATE, contact.ppl_birth.toString())
                        .build()
                )
            }
            if (contact.ppl_note.isNotBlank()) {
                add(
                    contPrvOpsActionInsertWithType(Note.CONTENT_ITEM_TYPE)
                        .withValue(Note.NOTE, contact.ppl_note)
                        .build()
                )
            }
            if (contact.orgs_id != 0) {
                add(
                    contPrvOpsActionInsertWithType(Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, contact.orgs_id)
                        .build()
                )
            }
            if (contact.ppl_mail.isNotBlank()) {
                add(
                    contPrvOpsActionInsertWithType(Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.ADDRESS, contact.ppl_mail)
                        .build()
                )
            }
            if (contact.ppl_mail2.isNotBlank()) {
                add(
                    contPrvOpsActionInsertWithType(Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.ADDRESS, contact.ppl_mail2)
                        .build()
                )
            }
            if (contact.ppl_phone1.isNotBlank()) {
                add(
                    contPrvOpsActionInsertWithType(Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, contact.ppl_phone1)
                        .build()
                )
            }
            if (contact.ppl_phone2.isNotBlank()) {
                add(
                    contPrvOpsActionInsertWithType(Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, contact.ppl_phone2)
                        .build()
                )
            }
            if (contact.ppl_address.isNotBlank()) {
                add(
                    contPrvOpsActionInsertWithType(StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(StructuredPostal.FORMATTED_ADDRESS, contact.ppl_address)
                        .withValue(StructuredPostal.CITY, contact.ppl_city)
                        .build()
                )
            }
            if (contact.ppl_address_live.isNotBlank()) {
                add(
                    contPrvOpsActionInsertWithType(StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(StructuredPostal.FORMATTED_ADDRESS, contact.ppl_address_live)
                        .withValue(StructuredPostal.CITY, contact.ppl_city)
                        .build()
                )
            }
        }

        try {
            contentResolver.applyBatch(AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Не удалось добавить контакты",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    private fun contPrvOpsActionInsertWithType(type: String): ContentProviderOperation.Builder {
        return ContentProviderOperation
            .newInsert(Data.CONTENT_URI)
            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, type)
    }

    private fun clearContactsBeforeInsert() {
        contentResolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts.CONTACT_ID),
            "${RawContacts.SYNC1} like ?",
            arrayOf("${SYNC_ID}%"),
            null
        )?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndex(RawContacts.CONTACT_ID))
                contentResolver.delete(Data.CONTENT_URI, "${Data.CONTACT_ID} = $id", null)
                contentResolver.delete(
                    RawContacts.CONTENT_URI,
                    "${RawContacts.CONTACT_ID} = $id",
                    null
                )
                contentResolver.delete(Contacts.CONTENT_URI, "${Contacts._ID} = $id", null)
            }
        }
    }


}
