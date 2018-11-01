package org.anyupload;

import org.grain.httpserver.HttpConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserFileAction implements IUserFileAction {
    public static String FILE_BASE_PATH;
    public static SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * 文件map uuid->UserFile
     */
    public static Map<String, UserFile> userFileMap = new ConcurrentHashMap<String, UserFile>();
    /**
     * 已经上传完成的文件map md5->FileBase
     */
    public static Map<String, FileBase> completeFileBaseMap = new ConcurrentHashMap<String, FileBase>();

    public static boolean stringIsNull(String str) {
        if (str == null || str.equals("")) {
            return true;
        }
        return false;
    }

    @Override
    public FileBase getFileBaseByMd5(String fileBaseMd5) {
        if (stringIsNull(fileBaseMd5)) {
            return null;
        }
        return completeFileBaseMap.get(fileBaseMd5);
    }

    public static String dateToStringDay(Date date) {
        if (date == null) {
            return null;
        }
        return shortDateFormat.format(date);
    }

    @Override
    public UserFile getUserFile(String userFileId) {
        if (stringIsNull(userFileId)) {
            return null;
        }
        return userFileMap.get(userFileId);
    }

    @Override
    public UserFile getUserFileComplete(String userFileId) {
        if (stringIsNull(userFileId)) {
            return null;
        }
        UserFile userFile = userFileMap.get(userFileId);
        if (userFile != null) {
            if (userFile.getFileBase().getFileBaseState() == FileBaseConfig.STATE_COMPLETE) {
                return userFile;
            }
        }
        return null;
    }

    @Override
    public UserFile createUserFile(String userFileName, String userFoldParentId, String createUserId, String fileBaseMd5, long fileBaseTotalSize, FileBase fileBase, String foldName) {
        if (stringIsNull(userFileName) || stringIsNull(fileBaseMd5)) {
            return null;
        }
        UserFile userFile = new UserFile();
        Date date = new Date();
        userFile.setUserFileId(UUID.randomUUID().toString().trim().replaceAll("-", ""));
        userFile.setUserFileName(userFileName);
        userFile.setUserFoldParentId(userFoldParentId);
        userFile.setUserFileCreateTime(date);
        userFile.setUserFileUpdateTime(date);
        userFile.setUserFileState(1);
        userFile.setCreateUserId(createUserId);
        if (fileBase == null) {
            FileBase newFileBase = new FileBase();
            newFileBase.setFileBaseId(UUID.randomUUID().toString().trim().replaceAll("-", ""));
            String fileBaseRealPath = createFile(foldName, getFileName(userFileName, newFileBase.getFileBaseId()));
            if (fileBaseRealPath == null) {
                return null;
            }
            newFileBase.setFileBaseRealPath(fileBaseRealPath);
            newFileBase.setFileBaseMd5(fileBaseMd5);
            newFileBase.setFileBaseState(FileBaseConfig.STATE_UPLOADING);
            newFileBase.setFileBaseTotalSize(fileBaseTotalSize);
            newFileBase.setFileBasePos(0L);
            newFileBase.setFileBaseCreateTime(date);
            newFileBase.setFileBaseNextUploadTime(date);
            userFile.setFileBaseId(newFileBase.getFileBaseId());
            userFile.setFileBase(newFileBase);
        } else {
            userFile.setFileBaseId(fileBase.getFileBaseId());
            userFile.setFileBase(fileBase);
        }
        userFileMap.put(userFile.getUserFileId(), userFile);
        return userFile;
    }

    @Override
    public boolean changeFileBase(UserFile userFile, FileBase fileBase) {
        userFile.setFileBaseId(fileBase.getFileBaseId());
        userFile.setFileBase(fileBase);
        return true;
    }

    @Override
    public boolean updateFile(File file, File chunkFile) {
        FileOutputStream fileOutputStream = null;
        FileInputStream fileInputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file, true);
            fileInputStream = new FileInputStream(chunkFile);
            byte[] buffer = new byte[CommonConfig.ONCE_WRITE_FILE_SIZE];
            int bytesRead = -1;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.flush();
            return true;
        } catch (Exception e) {
            HttpConfig.log.error("修改文件失败", e);
            return false;
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    HttpConfig.log.error("关闭块文件输入流失败", e);
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    HttpConfig.log.error("关闭输出流失败", e);
                }
            }
        }
    }

    @Override
    public boolean updateUserFile(UserFile userFile, int uploadLength) {
        Date date = new Date();
        userFile.getFileBase().setFileBasePos(userFile.getFileBase().getFileBasePos() + uploadLength);
        // 已完成
        if (userFile.getFileBase().getFileBasePos() == userFile.getFileBase().getFileBaseTotalSize()) {
            userFile.getFileBase().setFileBaseNextUploadTime(null);
            userFile.getFileBase().setFileBaseCompleteTime(date);
            userFile.getFileBase().setFileBaseState((byte) FileBaseConfig.STATE_COMPLETE);
            // 存入md5映射表
//            completeFileBaseMap.put(userFile.getFileBase().getFileBaseMd5(), userFile.getFileBase());
        } else {
            long fileBaseNextUploadTimeLong = date.getTime() + CommonConfig.WAIT_TIME;
            Date fileBaseNextUploadTime = new Date(fileBaseNextUploadTimeLong);
            userFile.getFileBase().setFileBaseNextUploadTime(fileBaseNextUploadTime);
        }
        return true;
    }


    @Override
    public void createFileBaseDir() {
        FILE_BASE_PATH = CommonConfig.FILE_BASE_PATH;
        File file = new File(FILE_BASE_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String getFileName(String userFileName, String fileBaseId) {
        int postfixIndex = userFileName.lastIndexOf(".");
        if (postfixIndex == -1) {
            return fileBaseId;
        } else {
            String postfix = userFileName.substring(postfixIndex);
            return fileBaseId + postfix;
        }
    }


    public static String createFile(String foldName, String fileName) {
        boolean result = createFold(foldName);
        if (!result) {
            return null;
        }
        File basePath = new File(FILE_BASE_PATH);
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        File file = new File(FILE_BASE_PATH + "/" + foldName + "/" + fileName);
        try {
            result = file.createNewFile();
            if (result) {
                return foldName + "/" + fileName;
            } else {
                return null;
            }
        } catch (IOException e) {
            HttpConfig.log.error("创建文件异常", e);
            return null;
        }
    }

    @Override
    public File getFile(String fileBaseRealPath) {
        File file = new File(FILE_BASE_PATH + "/" + fileBaseRealPath);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    public static boolean deleteFile(String fileBaseRealPath) {
        File file = new File(FILE_BASE_PATH + "/" + fileBaseRealPath);
        try {
            return file.delete();
        } catch (Exception e) {
            HttpConfig.log.error("删除文件异常", e);
            return false;
        }
    }

    public static boolean createFold(String name) {
        File file = new File(FILE_BASE_PATH + "/" + name);
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
            return true;
        } catch (Exception e) {
            HttpConfig.log.error("创建文件夹异常", e);
            return false;
        }
    }


}
