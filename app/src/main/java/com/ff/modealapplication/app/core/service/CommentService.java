package com.ff.modealapplication.app.core.service;

import com.ff.modealapplication.andorid.network.JSONResult;
import com.ff.modealapplication.app.core.domain.CommentVo;
import com.ff.modealapplication.app.core.util.Base;
import com.github.kevinsawicki.http.HttpRequest;

import java.net.HttpURLConnection;
import java.util.List;

public class CommentService {
    public List<CommentVo> commentInform(Long shopNo) {
        String url = Base.url + "modeal/commentapp/list";
        HttpRequest httpRequest = HttpRequest.post(url);

        httpRequest.contentType(HttpRequest.CONTENT_TYPE_FORM);     // 전달 타입
        httpRequest.accept(HttpRequest.CONTENT_TYPE_JSON);          // 받을 타입
        httpRequest.connectTimeout(15000);
        httpRequest.readTimeout(3000);

//        // 안드로이드에서 이클립스로 데이터 보내기
        httpRequest.send("shopNo="+shopNo);

        int responseCode = httpRequest.code();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Http Response : " + responseCode);
        }

        // 이클립스에서 안드로이드로 데이터 받기
        CommentInformation commentInformation = Base.fromJSON(httpRequest, CommentInformation.class);

        return commentInformation.getData();
    }

    public class CommentInformation extends JSONResult<List<CommentVo>> {
    }
}
