/*
Author: Найденов Антон BrHouse Lab.
email: brhouselab@gmail.com

Лицензия:
- Данное ПО можно свободно использовать для личных целей. Для коммерческих целей
необходимо получения лицензионного соглашения.
*/

package by.brhouselab.syncphone

import java.util.*


data class Contact(
    val ppl_id: Int = -1,
    val ppl_surn: String = "",
    val ppl_name: String = "",
    val ppl_maiden: String = "",
    val ppl_secd: String = "",
    val ppl_birth: Date = Date(),
    val ppl_city: Int = -1,
    val ppl_note: String = "",
    val ppl_mail: String = "",
    val ppl_mail2: String = "",
    val ppl_phone1: String = "",
    val ppl_phone2: String = "",
    val ppl_address: String = "",
    val ppl_address_live: String = "",
    val orgs_id: Int = -1
)