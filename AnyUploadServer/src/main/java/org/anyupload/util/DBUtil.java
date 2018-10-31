package org.anyupload.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    //    public static final String MODE = "dev";
//    public static final String MODE = "live";
//    public static final String MODE = "test";
    public static final String MODE = "local";


    private static final String URL_LOCAL = "jdbc:mysql://127.0.0.1:3306/wecloudDb1108?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    private static final String USER_LOCAL = "root";
    private static final String PASSWORD_LOCAL = "dtx60pp";

    private static final String URL_TEST = "jdbc:mysql://47.98.54.22:3306/wecloudDbTest?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    private static final String USER_TEST = "root";
    private static final String PASSWORD_TEST = "dtx60";

    private static final String URL_DEV = "jdbc:mysql://120.26.48.110:3306/wecloudDb1108?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    private static final String USER_DEV = "root";
    private static final String PASSWORD_DEV = "dtx60pp";

    private static final String URL_LIVE = "jdbc:mysql://47.98.54.22:3306/wecloudDb1108?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    private static final String USER_LIVE = "root";
    private static final String PASSWORD_LIVE = "dtx60";

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