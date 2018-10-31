package org.anyupload.entity;

import net.sf.json.JSONObject;
import org.anyupload.CommonConfig;
import org.anyupload.FileBase;

/**
 * @author cc
 * @date 18-10-30 上午12:45
 */
public class FileResult {
    String  hOpCode;
    int result;
    String userFileId;
    FileBase fileBase;
    Long fileBasePos ;
    Integer uploadMaxLength;
    Integer waitTime;

    @Override
    public String toString() {
        return JSONObject.fromObject(this).toString();
    }

    public String gethOpCode() {
        return hOpCode;
    }

    public int getResult() {
        return result;
    }

    public String getUserFileId() {
        return userFileId;
    }

    public FileBase getFileBase() {
        return fileBase;
    }

    public Long getFileBasePos() {
        return fileBasePos;
    }

    public Integer getUploadMaxLength() {
        return uploadMaxLength;
    }

    public Integer getWaitTime() {
        return waitTime;
    }

    private FileResult(Builder builder) {
        hOpCode = builder.hOpCode;
        result = builder.result;
        userFileId = builder.userFileId;
        fileBase = builder.fileBase;
        fileBasePos = builder.fileBasePos;
        uploadMaxLength = builder.uploadMaxLength;
        waitTime = builder.waitTime;
    }


    public static final class Builder {
        private String hOpCode;
        private int result;
        private String userFileId;
        private FileBase fileBase;
        private Long fileBasePos;
        private Integer uploadMaxLength;
        private Integer waitTime;

        public Builder() {
        }

        public Builder hOpCode(String val) {
            hOpCode = val;
            return this;
        }

        public Builder result(int val) {
            result = val;
            return this;
        }

        public Builder userFileId(String val) {
            userFileId = val;
            return this;
        }

        public Builder fileBase(FileBase val) {
            fileBase = val;
            return this;
        }

        public Builder fileBasePos(Long val) {
            fileBasePos = val;
            return this;
        }

        public Builder uploadMaxLength(Integer val) {
            uploadMaxLength = val;
            return this;
        }

        public Builder waitTime(Integer val) {
            waitTime = val;
            return this;
        }

        public FileResult build() {
            return new FileResult(this);
        }
    }
}
