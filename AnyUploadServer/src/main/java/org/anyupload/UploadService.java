package org.anyupload;

import net.sf.json.JSONObject;
import org.anyupload.entity.FileResult;
import org.anyupload.entity.UserToken;
import org.anyupload.protobuf.ErrorProto.ErrorCode;
import org.anyupload.protobuf.ErrorProto.ErrorS;
import org.anyupload.protobuf.UploadFileProto.MD5CheckC;
import org.anyupload.protobuf.UploadFileProto.UploadFileC;
import org.anyupload.util.JwtUtil;
import org.grain.httpserver.*;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UploadService implements IHttpListener {
    public IUserFileAction userFileAction;

    @Override
    public Map<String, String> getHttps() {
        HashMap<String, String> map = new HashMap<>();
        map.put(HOpCode.MD5_CHECK, "md5CheckHandle");
        map.put(HOpCode.UPLOAD_FILE, "uploadFileHandle");
        return map;
    }

    public ReplyString md5CheckHandle(HttpPacket httpPacket) throws HttpException {
//        JSONObject params = JSONObject.fromObject(httpPacket.hSession.headParam.token);
//        String token = params.getString("Authorization");
//        String foldName = null;
//        try {
//            UserToken userToken = checkUser(token, httpPacket.gethOpCode());
//            foldName = userToken.getUserId() + "-" + params.get("timestamp").toString();
//        } catch (HttpException e) {
//            e.printStackTrace();
//        }
        String foldName = "22222";
        MD5CheckC message = (MD5CheckC) httpPacket.getData();

        UserFile userFile = null;
        // 不为空
        if (message.getUserFileId() != null && !message.getUserFileId().equals("")) {
            // 获取文件
            userFile = userFileAction.getUserFile(message.getUserFileId());
            // 如果已经完成，秒传
            if (userFile != null && userFile.getFileBase().getFileBaseState() == FileBaseConfig.STATE_COMPLETE) {
                return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).result(1)
                        .userFileId(userFile.getUserFileId()).fileBase(userFile.getFileBase()).build().toString(), "application/json");
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
                return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).result(1)
                        .userFileId(userFile.getUserFileId()).fileBase(userFile.getFileBase()).build().toString(), "application/json");
            } else {
                // 通知从哪开始传
                return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).userFileId(userFile.getUserFileId()).result(2)
                        .fileBasePos(userFile.getFileBase().getFileBasePos()).uploadMaxLength(CommonConfig.UPLOAD_MAX_LENGTH)
                        .fileBase(userFile.getFileBase()).build().toString(), "application/json");
            }
        } else {
            if (fileBase != null) {
                // 更新，然后秒传
                boolean result = userFileAction.changeFileBase(userFile, fileBase);
                if (!result) {
                    ErrorS boxErrorS = createError(ErrorCode.ERROR_CODE_0, httpPacket.gethOpCode());
                    throw new HttpException(HOpCode.ERROR, boxErrorS);
                }
                return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).result(1)
                        .userFileId(userFile.getUserFileId()).fileBase(userFile.getFileBase()).build().toString(), "application/json");
            } else {
                // 通知从哪开始传
                return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).result(2)
                        .fileBasePos(userFile.getFileBase().getFileBasePos()).uploadMaxLength(CommonConfig.UPLOAD_MAX_LENGTH)
                        .userFileId(userFile.getUserFileId()).fileBase(userFile.getFileBase()).build().toString(), "application/json");
            }
        }
    }

    public ReplyString uploadFileHandle(HttpPacket httpPacket) throws HttpException {
//        JSONObject params = JSONObject.fromObject(httpPacket.hSession.headParam.token);
//        String token = params.getString("Authorization");
//        String foldName = null;
//        try {
//            UserToken userToken = checkUser(token, httpPacket.gethOpCode());
//            foldName = userToken.getUserId() + "-" + params.get("timestamp").toString();
//        } catch (HttpException e) {
//            e.printStackTrace();
//        }
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
            return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).result(1)
                    .userFileId(userFile.getUserFileId()).fileBase(userFile.getFileBase()).build().toString(), "application/json");
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
            return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).result(3)
                    .userFileId(userFile.getUserFileId()).fileBase(userFile.getFileBase()).build().toString(), "application/json");
        } else {
            return new ReplyString(new FileResult.Builder().hOpCode(httpPacket.gethOpCode()).result(1)
                    .fileBasePos(userFile.getFileBase().getFileBasePos()).uploadMaxLength(CommonConfig.UPLOAD_MAX_LENGTH)
                    .waitTime(CommonConfig.WAIT_TIME)
                    .userFileId(userFile.getUserFileId()).fileBase(userFile.getFileBase()).build().toString(), "application/json");
        }
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
}
