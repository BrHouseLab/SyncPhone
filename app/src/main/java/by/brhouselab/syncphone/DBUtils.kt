/*
Author: Найденов Антон BrHouse Lab.
email: brhouselab@gmail.com

Лицензия:
- Данное ПО можно свободно использовать для личных целей. Для коммерческих целей
необходимо получения лицензионного соглашения.
*/

package by.brhouselab.syncphone

import java.sql.Connection
import java.sql.DriverManager

class DBUtils {
    companion object {
        fun getConnection(host: String, login: String, pass: String): Connection {
            Class.forName("com.mysql.jdbc.Driver").newInstance()
            return DriverManager.getConnection("jdbc:mysql://${host}?useUnicode=true&characterEncoding=utf8", login, pass)
        }
    }


}