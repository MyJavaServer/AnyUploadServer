package org.anyupload;

import net.sf.json.JSONObject;
import org.anyupload.entity.UserToken;
import org.anyupload.protobuf.ErrorProto.ErrorCode;
import org.anyupload.protobuf.ErrorProto.ErrorS;
import org.anyupload.protobuf.UploadFileProto.MD5CheckC;
import org.anyupload.protobuf.UploadFileProto.MD5CheckS;
import org.anyupload.protobuf.UploadFileProto.UploadFileC;
import org.anyupload.protobuf.UploadFileProto.UploadFileS;
import org.anyupload.util.DBUtil;
import org.anyupload.util.JwtUtil;
import org.grain.httpserver.*;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.anyupload.UserFileAction.completeFileBaseMap;
import static org.anyupload.UserFileAction.userFileMap;

public class UploadService implements IHttpListener {
    public IUserFileAction userFileAction;

    @Override
    public Map<String, String> getHttps() {
        HashMap<String, String> map = new HashMap<>();
        map.put(HOpCode.MD5_CHECK, "md5CheckHandle");
        map.put(HOpCode.UPLOAD_FILE, "uploadFileHandle");
        map.put(HOpCode.DELETE_FILE, "deleteFileHandle");
        map.put(HOpCode.DELETE_FOLD, "deleteFoldHandle");
        return map;
    }

    public HttpPacket md5CheckHandle(HttpPacket httpPacket) throws HttpException {
        JSONObject params = JSONObject.fromObject(httpPacket.hSession.headParam.token);
        String token = params.getString("Authorization");
        String foldName = null;
        UserToken userToken = null;
        try {
            userToken = checkUser(token, httpPacket.gethOpCode());
            foldName = userToken.getUserId() + "-" + params.get("timestamp").toString();
        } catch (HttpException e) {
            e.printStackTrace();
        }
        foldName = "/" + userToken.getUserId() + "/system/datasets/" + foldName;
        MD5CheckC message = (MD5CheckC) httpPacket.getData();

        UserFile userFile = null;
        // 不为空
        if (message.getUserFileId() != null && !message.getUserFileId().equals("")) {
            // 获取文件
            userFile = userFileAction.getUserFile(message.getUserFileId());
            // 如果已经完成，秒传
            if (userFile != null && userFile.getFileBase().getFileBaseState() == FileBaseConfig.STATE_COMPLETE) {
                MD5CheckS.Builder builder = MD5CheckS.newBuilder();
                builder.setHOpCode(httpPacket.gethOpCode());
                builder.setResult(1);
                builder.setUserFileId(userFile.getUserFileId());
                HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
                return packet;
            }
        }
        // 取已经完成的基础文件
        FileBase fileBase = userFileAction.getFileBaseByMd5(message.getFileBaseMd5());
        if (userFile == null) {
            // 文件为空，则创建文件
            userFile = userFileAction.createUserFile(message.getUserFileName(), message.getUserFoldParentId(), null, message.getFileBaseMd5(), message.getFileBaseTotalSize(), fileBase, foldName);
            if (userFile == null) {
                ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_0, httpPacket.gethOpCode());
                throw new HttpException(HOpCode.ERROR, boxErrorS);
            }
            if (fileBase != null) {
                // 秒传
                MD5CheckS.Builder builder = MD5CheckS.newBuilder();
                builder.setHOpCode(httpPacket.gethOpCode());
                builder.setResult(1);
                builder.setUserFileId(userFile.getUserFileId());
                HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
                return packet;
            } else {
                // 通知从哪开始传
                MD5CheckS.Builder builder = MD5CheckS.newBuilder();
                builder.setHOpCode(httpPacket.gethOpCode());
                builder.setResult(2);
                builder.setUserFileId(userFile.getUserFileId());
                builder.setFileBasePos(userFile.getFileBase().getFileBasePos());
                builder.setUploadMaxLength(CommonConfig.UPLOAD_MAX_LENGTH);
                HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
                return packet;
            }
        } else {
            if (fileBase != null) {
                // 更新，然后秒传
                boolean result = userFileAction.changeFileBase(userFile, fileBase);
                if (!result) {
                    ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_0, httpPacket.gethOpCode());
                    throw new HttpException(HOpCode.ERROR, boxErrorS);
                }
                MD5CheckS.Builder builder = MD5CheckS.newBuilder();
                builder.setHOpCode(httpPacket.gethOpCode());
                builder.setResult(1);
                builder.setUserFileId(userFile.getUserFileId());
                HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
                return packet;
            } else {
                // 通知从哪开始传
                MD5CheckS.Builder builder = MD5CheckS.newBuilder();
                builder.setHOpCode(httpPacket.gethOpCode());
                builder.setResult(2);
                builder.setUserFileId(userFile.getUserFileId());
                builder.setFileBasePos(userFile.getFileBase().getFileBasePos());
                builder.setUploadMaxLength(CommonConfig.UPLOAD_MAX_LENGTH);
                HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
                return packet;
            }
        }
    }

    public HttpPacket uploadFileHandle(HttpPacket httpPacket) throws HttpException {
        JSONObject params = JSONObject.fromObject(httpPacket.hSession.headParam.token);
        String token = params.getString("Authorization");
        try {
            UserToken userToken = checkUser(token, httpPacket.gethOpCode());
        } catch (HttpException e) {
            e.printStackTrace();
        }
        UploadFileC message = (UploadFileC) httpPacket.getData();
        UserFile userFile = userFileAction.getUserFile(message.getUserFileId());
        if (userFile == null) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_1, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        // 位置不对
        if (message.getFileBasePos() != userFile.getFileBase().getFileBasePos()) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_2, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        // 不能大于默认长度
        if (message.getUploadLength() > CommonConfig.UPLOAD_MAX_LENGTH) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_3, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        // 没有文件
        if (httpPacket.fileList == null || httpPacket.fileList.size() == 0) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_4, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        FileData fileData = httpPacket.fileList.get(0);
        // 文件长度与消息长度不符
        if ((int) fileData.getFile().length() != message.getUploadLength()) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_3, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        // 不存在这个文件
        File file = userFileAction.getFile(userFile.getFileBase().getFileBaseRealPath());
        if (file == null) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_5, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        if (file.length() != message.getFileBasePos()) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_3, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        Date date = new Date();
        // 是否在规定时间后上传
        if (date.getTime() < userFile.getFileBase().getFileBaseNextUploadTime().getTime()) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_6, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        FileBase fileBase = userFileAction.getFileBaseByMd5(userFile.getFileBase().getFileBaseMd5());
        if (fileBase != null) {
            // 更新，然后秒传
            boolean result = userFileAction.changeFileBase(userFile, fileBase);
            if (!result) {
                ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_7, httpPacket.gethOpCode());
                throw new HttpException(HOpCode.ERROR, boxErrorS);
            }
            UploadFileS.Builder builder = UploadFileS.newBuilder();
            builder.setHOpCode(httpPacket.gethOpCode());
            builder.setResult(1);
            builder.setUserFileId(userFile.getUserFileId());
            HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
            return packet;
        }
        boolean result = userFileAction.updateFile(file, fileData.getFile());
        if (!result) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_8, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        result = userFileAction.updateUserFile(userFile, message.getUploadLength());
        if (!result) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_9, httpPacket.gethOpCode());
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        }
        if ((message.getFileBasePos() + message.getUploadLength()) == userFile.getFileBase().getFileBaseTotalSize()) {
            UploadFileS.Builder builder = UploadFileS.newBuilder();
            builder.setHOpCode(httpPacket.gethOpCode());
            builder.setResult(3);
            builder.setUserFileId(userFile.getFileBase().getFileBaseRealPath().split("/system/datasets/")[1]);
            HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
            try {
                saveFile(userFile.getUserFileName(), userFile.getFileBase().getFileBaseRealPath().split("/system/datasets/")[1], String.valueOf(userFile.getFileBase().getFileBaseTotalSize()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            completeFileBaseMap.remove(userFile.getFileBase().getFileBaseMd5());
            userFileMap.remove(userFile.getUserFileId());
            return packet;
        } else {
            UploadFileS.Builder builder = UploadFileS.newBuilder();
            builder.setHOpCode(httpPacket.gethOpCode());
            builder.setResult(2);
            builder.setUserFileId(userFile.getUserFileId());
            builder.setWaitTime(CommonConfig.WAIT_TIME);
            builder.setUploadMaxLength(CommonConfig.UPLOAD_MAX_LENGTH);
            builder.setFileBasePos((message.getFileBasePos() + message.getUploadLength()));
            HttpPacket packet = new HttpPacket(httpPacket.gethOpCode(), builder.build());
            return packet;
        }
    }

    public ReplyString deleteFileHandle(HttpPacket httpPacket) {
        JSONObject params = JSONObject.fromObject(httpPacket.hSession.headParam.token);
        String token = params.getString("Authorization");
        String fileId = null;
        UserToken userToken = null;
        JSONObject jsonObject = new JSONObject();
        try {
            userToken = checkUser(token, httpPacket.gethOpCode());
            fileId = params.getString("fileId");
        } catch (HttpException e) {
            e.printStackTrace();
        }
        if (!fileId.startsWith(userToken.getUserId() + "")) {
            jsonObject.put("status", "0");
            jsonObject.put("result", "无权限！");
            return new ReplyString(jsonObject.toString(), "application/json");
        }
       String foldName = "/" + userToken.getUserId() + "/system/datasets/" + fileId;
        UserFileAction.deleteFile(foldName);
        try {
            deleteFile(fileId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        jsonObject.put("status", "1");
        jsonObject.put("result", "删除成功！");
        return new ReplyString(jsonObject.toString(), "application/json");
    }

    public ReplyString deleteFoldHandle(HttpPacket httpPacket) {
        JSONObject params = JSONObject.fromObject(httpPacket.hSession.headParam.token);
        String token = params.getString("Authorization");
        String foldName = params.getString("fileName");
        int userid = params.getInt("userId");
        JSONObject jsonObject = new JSONObject();
        foldName = "/" + userid + "/system/datasets/" + foldName;
        if (!"ohcloud@123".equals(token)) {
            jsonObject.put("status", "0");
            jsonObject.put("result", "无权限！");
            return new ReplyString(jsonObject.toString(), "application/json");
        }
        UserFileAction.deleteFile(foldName);
        jsonObject.put("status", "1");
        jsonObject.put("result", "删除成功！");
        return new ReplyString(jsonObject.toString(), "application/json");
    }

    public UploadService() {
        userFileAction = new UserFileAction();
        userFileAction.createFileBaseDir();
    }

    public static ErrorS createError(ErrorCode boxErrorCode, String errorHOpCode) {
        ErrorS.Builder errorBuilder = ErrorS.newBuilder();
        errorBuilder.setHOpCode(HOpCode.ERROR);
        errorBuilder.setErrorCode(boxErrorCode);
        errorBuilder.setErrorHOpCode(errorHOpCode);
        return errorBuilder.build();
    }

    public UserToken checkUser(String tokenStr, String code) throws HttpException {
        UserToken userToken = JwtUtil.parseJWT(tokenStr, "ocloud");
        if (userToken == null) {
            ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_10, code);
            throw new HttpException(HOpCode.ERROR, boxErrorS);
        } else {
            return userToken;
        }

    }

    private void saveFile(String name, String path, String size) throws SQLException {
        Connection conn = DBUtil.getConnection();
        String s = " " + "insert into dl_dset_file(name,path,size) values(" + "?,?,?)";
        PreparedStatement pst = conn.prepareStatement(s);
        pst.setString(1, name);
        pst.setString(2, path);
        pst.setString(3, size);
        pst.execute();
        //关闭资源
        pst.close();
    }

    private void deleteFile(String path) throws SQLException {
        Connection conn = DBUtil.getConnection();
        String s = " " + "delete from dl_dset_file where path = ?";
        PreparedStatement pst = conn.prepareStatement(s);
        pst.setString(1, path);
        pst.execute();
        //关闭资源
        pst.close();
    }
}
