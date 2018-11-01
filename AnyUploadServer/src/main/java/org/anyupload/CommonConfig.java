package org.anyupload;

public class CommonConfig {
    /**
     * 基础文件路径
     */
    public static String FILE_BASE_PATH = "/notebook/storage/";
    /**
     * 最大上传长度
     */
    public static int UPLOAD_MAX_LENGTH = 6553600;
    /**
     * 客户端下一次上传文件间隔
     */
    public static int WAIT_TIME = 640;
    /**
     * 一次性写入文件的大小
     */
    public static int ONCE_WRITE_FILE_SIZE = 6553600;


    public static final String MODE = "dev";
//    public static final String MODE = "live";
//    public static final String MODE = "test";
//	public static final String MODE = "local";


    public static final String URL_LOCAL = "jdbc:mysql://127.0.0.1:3306/wecloudDb1108?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    public static final String USER_LOCAL = "root";
    public static final String PASSWORD_LOCAL = "dtx60pp";

    public static final String URL_TEST = "jdbc:mysql://47.98.54.22:3306/wecloudDbTest?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    public static final String USER_TEST = "root";
    public static final String PASSWORD_TEST = "dtx60";

    public static final String URL_DEV = "jdbc:mysql://120.26.48.110:3306/wecloudDb1108?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    public static final String USER_DEV = "root";
    public static final String PASSWORD_DEV = "dtx60pp";

    public static final String URL_LIVE = "jdbc:mysql://47.98.54.22:3306/wecloudDb1108?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai";
    public static final String USER_LIVE = "root";
    public static final String PASSWORD_LIVE = "dtx60";


}
