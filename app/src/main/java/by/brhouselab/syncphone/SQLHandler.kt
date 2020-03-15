/*
Author: Найденов Антон BrHouse Lab.
email: brhouselab@gmail.com

Лицензия:
- Данное ПО можно свободно использовать для личных целей. Для коммерческих целей
необходимо получения лицензионного соглашения.
*/

package by.brhouselab.syncphone

import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import java.sql.Connection
import java.util.*
import kotlin.collections.ArrayList

class SQLHandler(val table: String, val progress: Handler) :
    AsyncTask<String, String, List<Contact>>() {

    private val DB_REQUEST = "select * from $table"
    var resultMessage = ""

    override fun doInBackground(vararg params: String?): List<Contact> {
        try {
            DBUtils.getConnection(
                params[0].orEmpty(),
                params[1].orEmpty(),
                params[2].orEmpty()
            )
                .use {
                    resultMessage = "Установлено соединение. Подготовка к синхронизации"
                    return getContacts(it)
                }
        } catch (e: Exception) {
            resultMessage = "Не удалось подключиться к базе данных"
        }

        return emptyList()
    }

    private fun getContacts(conn: Connection): List<Contact> {
        val state = conn.prepareStatement(DB_REQUEST).executeQuery()
        val list = ArrayList<Contact>()
        try {
            state.use {
                it.last()
                val maxRow = it.row
                it.beforeFirst()
                progress.sendMessage(Message().apply { arg1 = 0; arg2 = maxRow })
                var currentRow = 0
                while (it.next()) {
                    list.add(
                        Contact(
                            ppl_id = it.getInt("ppl_id").or(-1),
                            ppl_surn = it.getString("ppl_surn").orEmpty(),
                            ppl_name = it.getString("ppl_name").orEmpty(),
                            ppl_maiden = it.getString("ppl_maiden").orEmpty(),
                            ppl_secd = it.getString("ppl_secd").orEmpty(),
                            ppl_birth = it.getDate("ppl_birth") ?: Date(0),
                            ppl_city = it.getInt("ppl_city").or(-1),
                            ppl_note = it.getString("ppl_note").orEmpty(),
                            ppl_mail = it.getString("ppl_mail").orEmpty(),
                            ppl_mail2 = it.getString("ppl_mail2").orEmpty(),
                            ppl_phone1 = it.getString("ppl_phone1").orEmpty(),
                            ppl_phone2 = it.getString("ppl_phone2").orEmpty(),
                            ppl_address = it.getString("ppl_address").orEmpty(),
                            ppl_address_live = it.getString("ppl_address_live").orEmpty(),
                            orgs_id = it.getInt("orgs_id").or(-1)
                        )
                    )

                    progress.sendMessage(Message().apply { arg1 = currentRow++; arg2 = maxRow })
                }
            }
        } catch (e: Exception) {
            resultMessage = "Ошибка сопоставления схемы. ${e.message}"
        }
        return list
    }
}