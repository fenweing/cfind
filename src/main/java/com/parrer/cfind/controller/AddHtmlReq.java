package com.parrer.cfind.controller;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AddHtmlReq {
    @NotNull(message = "url不能为空！")
    private String url;
    @NotNull(message = "html内容不能为空！")
    private String html;
    @NotNull(message = "title不能为空！")
    private String title;

    public static AddHtmlReq of() {
        return new AddHtmlReq();
    }


    public AddHtmlReq withUrl(String url) {
        setUrl(url);
        return this;
    }

    public AddHtmlReq withHtml(String html) {
        setHtml(html);
        return this;
    }

    public AddHtmlReq withTitle(String title) {
        setTitle(title);
        return this;
    }

}
