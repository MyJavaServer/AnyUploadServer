package org.anyupload.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.anyupload.CommonConfig.*;

public class DBUtil {

    private static Connection conn = null;

    static {
        String URL = null, USER = null, PASSWORD = null;
        switch (MODE) {
            case "dev":
                URL = URL_DEV;
                USER = USER_DEV;
                PASSWORD = PASSWORD_DEV;
                break;
            case "test":
                URL = URL_TEST;
                USER = USER_TEST;
                PASSWORD = PASSWORD_TEST;
                break;
            case "live":
                URL = URL_LIVE;
                USER = USER_LIVE;
                PASSWORD = PASSWORD_LIVE;
                break;
            case "local":
                URL = URL_LOCAL;
                USER = USER_LOCAL;
                PASSWORD = PASSWORD_LOCAL;
                break;
            default:
                URL = URL_LOCAL;
                USER = USER_LOCAL;
                PASSWORD = PASSWORD_LOCAL;
                break;
        }
        try {
            //1.加载驱动程序
            Class.forName("com.mysql.jdbc.Driver");
            //2.获得数据库的连接
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //将获得的数据库与java的链接返回（返回的类型为Connection）
    public static Connection getConnection() {
        return conn;
    }
}