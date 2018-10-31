package org.anyupload;

public class CommonConfig {
	/**
	 * 基础文件路径
	 */
	public static String FILE_BASE_PATH = "/notebook/storage/file/datasets/";
	/**
	 * 最大上传长度
	 */
	public static int UPLOAD_MAX_LENGTH = Integer.MAX_VALUE;
	/**
	 * 客户端下一次上传文件间隔
	 */
	public static int WAIT_TIME = 640;
	/**
	 * 一次性写入文件的大小
	 */
	public static int ONCE_WRITE_FILE_SIZE = 6553600;

	public static void main(String[] args) {
		System.out.println(Integer.MAX_VALUE);
	}
}
