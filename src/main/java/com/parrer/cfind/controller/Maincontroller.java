package com.parrer.cfind.controller;

import com.parrer.cfind.MarkdownUtils;
import com.parrer.util.AssertUtil;
import com.parrer.util.CollectionUtil;
import com.parrer.util.DateUtil;
import com.parrer.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RestController
@RequestMapping("/cfind")
public class Maincontroller {
    @Value("${cfind.command:/cfind/cfind.sh}")
    private String findCommandPath;
    @Value("${cfind.oriFilePath:/cfind/file}")
    private String oriFilePath;

    @GetMapping("/search/{keyword}")
    public ResponseEntity getByKeyword(@PathVariable("keyword") String keyword) {
        LogUtil.apiEntry(keyword);
        if (StringUtils.isBlank(keyword)) {
            log.error("blank keyword!");
            return ResponseEntity.ok().build();
        }
//        String html = "一、软件下载 >>->navicat<-<<\n" +
//                "[下载patch包](http://cloud.tuanbaol.com/s/KdYp9Xc54wQr9fb)\n" +
//                "\n" +
//                "二、软件安装\n" +
//                "双击运行Navicat Premium 15\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-3b6e9085e4ab449fbaf7cdbb1a032271.png)\n" +
//                " \n" +
//                "\n" +
//                " 选择软件安装路径\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-3fccba83e4e845cd91691b9c7b6bfed1.png)\n" +
//                " \n" +
//                "\n" +
//                "接下来一直点击下一步，最后点击完成\n" +
//                "\n" +
//                "安装完成后不要运行软件\n" +
//                "\n" +
//                "三、开始激活\n" +
//                "进行破解前，请先关闭电脑的杀毒软件\n" +
//                "\n" +
//                "双击运行注册机\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-3d2d763fb7774ee7badda82cf2a2252b.png)\n" +
//                "\n" +
//                "如果你的电脑只安装了一个navicat的产品，在打开注册机的时候一般会自动识别\n" +
//                "\n" +
//                "如果没有自动识别，就按照上面的图片勾选\n" +
//                "\n" +
//                "然后点击patch(破解)\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-ee8325e9c00a4233be4fac326264c5a8.png)\n" +
//                "\n" +
//                " \n" +
//                "\n" +
//                " 找到你软件的安装路径，然后打开即可\n" +
//                "\n" +
//                "找不到安装路径的朋友可以右键图标，选择打开文件路径\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-37c556b2ad2c4afbbdf3ecf0020531d9.png)\n" +
//                " \n" +
//                "\n" +
//                " 弹出这个提示，就是破解成功，点击确定\n" +
//                "\n" +
//                "接着点击Generate生成激活码\n" +
//                "\n" +
//                "然后再点击copy复制激活码\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-d55de174a08d46a7b29704561408d1c8.png)\n" +
//                " \n" +
//                "\n" +
//                " 双击运行Navicat Premium 15，选择注册\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-01a26903d5d04feea13e998976cd8022.png)\n" +
//                "\n" +
//                "\n" +
//                " \n" +
//                "\n" +
//                " ctrl+v粘贴激活码，点击激活\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-a9aacbd319db4de5ab86adcefe99e790.png)\n" +
//                "\n" +
//                " \n" +
//                "\n" +
//                " 然后点击手动激活\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-63f13ed44fb941d1abe388f96c7a5851.png)\n" +
//                " \n" +
//                "\n" +
//                " 右键全选复制请求码\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-28a84fecf2374c10bd326cda7b88cee6.png)\n" +
//                " \n" +
//                "\n" +
//                " 然后在注册机中填写请求码，点击Generate生成激活码，再点击copy复制激活码\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-1287867566774642a88217c37bd7b59e.png)\n" +
//                " \n" +
//                "\n" +
//                " 复制后将激活码填入navicat中\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-55ed012db73143ccb9c6af4f7c060147.png)\n" +
//                " \n" +
//                "\n" +
//                " 点击激活，破解完成\n" +
//                "\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-0c8f15b0092f48f7987d31a5d37c6c5b.png)\n" +
//                " \n" +
//                "\n" +
//                " 最后点击确定即可\n" +
//                "\n" +
//                "![image.png](http://www.tuanbaol.com/upload/2020/11/image-bf9cc2a02d364a0f8d78cbc54c20532d.png)\n" +
//                "\n" +
//                "原文：https://www.cnblogs.com/zenglintao/p/12823285.html\n";
//        html=MarkdownUtils.renderHtml(html);
        String html = getAndResolve(keyword);
        return ResponseEntity.ok(html);
    }

    @PostMapping("/add")
    public ResponseEntity addReference(@RequestBody String reference) {
        LogUtil.apiEntry(reference);
        if (isBlank(reference)) {
            log.error("blank reference!");
            return ResponseEntity.ok().build();
        }
        reference=reference.replace("%0A","\r\n");
        int beginIdx = reference.indexOf(">>->");
        int endIdx = reference.indexOf("<-<<");
        String date = DateUtil.formatYYYYMMDD(new Date());
        String dateTime = DateUtil.format(new Date());
        if (beginIdx > -1 && endIdx > -1) {
            AssertUtil.isFalse(endIdx - beginIdx <= 2, "bad format!");
            String keyword = reference.substring(beginIdx + 4, endIdx);
            reference = "*" + keyword + "-begin*\r\n\r\n"
                    + "> date: " + dateTime + "\r\n\r\n"
                    + reference + "\r\n" + "*" + keyword + "-end*";
        } else {
            reference = "*" + date + "-begin*\r\n\r\n"
                    + "> date: " + dateTime + "\r\n\r\n"
                    + reference + "\r\n" + "*" + date + "-end*";
        }
        reference="\r\n\r\n"+reference;
        File file = new File(oriFilePath);
        try {
            if (!file.exists()) {
                log.info("ori file not exists,create one!");
                boolean newFile = file.createNewFile();
                AssertUtil.isTrue(newFile, "create new ori file failed!");
            }
            FileUtils.write(file, reference, StandardCharsets.UTF_8, true);
        } catch (Exception e) {
            log.error("error occurred when write reference to orifile!", e);
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }

    private String getAndResolve(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return StringUtils.EMPTY;
        }
        String fromLinux = getFromLinux(keyword);
        if (StringUtils.isBlank(fromLinux)) {
            return StringUtils.EMPTY;
        }
        return MarkdownUtils.renderHtml(fromLinux);
    }

    private String getFromLinux(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return StringUtils.EMPTY;
        }
        Runtime run = Runtime.getRuntime();

        //insure configuration
        try {
            File file = new File(findCommandPath);
            if (!file.exists() || file.isDirectory()) {
                log.info("find command not exists,create default one!");
                File defaultFile = new File("cfind_default.sh");
                boolean newFile = file.createNewFile();
                AssertUtil.isTrue(newFile, "create default command file failed!");
                FileUtils.copyFile(defaultFile, file);
            }
        } catch (Exception e) {
            log.error("error occurred when check configuration！", e);
            return StringUtils.EMPTY;
        }

        //get from linux
        try {
            String param = "sh "+findCommandPath+" "+keyword;
//            Process process = run.exec(new String[]{"/bin/sh", "-c", findCommandPath, keyword});
            Process process = run.exec(param);
            InputStream in = process.getInputStream();
            BufferedReader bs = new BufferedReader(new InputStreamReader(in));
            ArrayList<String> partList = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            String result = null;
            while ((result = bs.readLine()) != null) {
                if(StringUtils.equals("//delimit//",result)){
                    stringBuilder.append("\r\n\r\n");
                    partList.add(stringBuilder.toString());
                    stringBuilder.delete(0,stringBuilder.length());
                    continue;
                }
                stringBuilder.append("\r\n").append(result);
            }
            in.close();
            process.destroy();
            if(stringBuilder.length()>0){
                partList.add(stringBuilder.toString());
            }
            List<String> reverse = CollectionUtil.reverse(partList);
            return String.join("\r\n",reverse);
        } catch (IOException e) {
            log.error("error occurred when get from linux!", e);
            return StringUtils.EMPTY;
        }
    }
}
