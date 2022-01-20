package com.parrer.cfind;

import com.parrer.cfind.controller.AddHtmlReq;
import com.parrer.scriptGenerate.JavaScriptGenerate;
import com.parrer.util.CollcUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

//@SpringBootTest
class CfindApplicationTests {
    public static void main(String[] args) {
        JavaScriptGenerate javaScriptGenerate = JavaScriptGenerate.of(CfindApplicationTests.class);
        LinkedList classes = CollcUtil.ofLinkedList(AddHtmlReq.class);
        javaScriptGenerate.generateStaticBuildMethod(classes);
        javaScriptGenerate.generateChainMethod(classes);
    }

    @Test
    void contextLoads() {
    }

}
