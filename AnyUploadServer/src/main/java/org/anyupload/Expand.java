package org.anyupload;

import org.grain.httpserver.*;

import javax.servlet.http.HttpServlet;

public class Expand implements IExpandServer {

    @Override
    public void init(HttpServlet servlet) throws Exception {
        // 初始化映射
        HOpCode.init();
        // 初始化服务
        HttpManager.addHttpListener(new UploadService());
    }
}
