package com.parrer.cfind.controller;

import com.parrer.cfind.MarkdownUtils;
import com.parrer.util.AssertUtil;
import com.parrer.util.CollectionUtil;
import com.parrer.util.DateUtil;
import com.parrer.util.LogUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RestController
@RequestMapping("/cfind")
public class Maincontroller {
    @Value("${cfind.command:/cfind/cfind.sh}")
    private String findCommandPath;
    @Value("${cfind.oriFilePath:/cfind/file}")
    private String oriFilePath;
    @Value("${cfind.attachFileDir:/cfind/attach}")
    private String attachFileDir;
    @Value("${cfind.docFileDir:/cfind/doc}")
    private String docFileDir;

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
        MainResponse response = getAndResolve(keyword);
//        String html="<div><img src=\"/cfind/getAttach/20210828182837\"></div>";
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity addReference(@RequestBody Map<String, String> param) {
        String type = param.get("type");
        log.info("/add 接口进入，type-{}", type);
//        log.info("/add 接口进入，param-{}", JsonUtil.toString(param));
        String reference = param.get("reference");
        type = isBlank(type) ? "html" : type;
        if (isBlank(reference)) {
            log.error("blank reference!");
            return ResponseEntity.ok().build();
        }
        reference = reference.replace("%0A", "\r\n");
        File file = null;
        if ("file".equals(type)) {
            String filename = param.get("filename");
            AssertUtil.notEmpty(filename, "filename can not be blank!");
            if (StringUtils.startsWith(filename, "f:")) {
                filename = filename.substring(2);
                AssertUtil.notEmpty(filename, "filename can not be blank!");
            }
            file = new File(docFileDir + "/" + filename);
            reference="<p>_type[file]</p>\r\n"+reference;
        } else {
            //获取第一行
            String baser = reference.split("\r")[0];
            String basen = reference.split("\n")[0];
            String firstLine = baser.length() > basen.length() ? basen : baser;
            //获取第一行结束
            int kwIdx = firstLine.indexOf("??");
            String date = DateUtil.formatYYYYMMDD(new Date());
            String dateTime = DateUtil.format(new Date());
            if (kwIdx > -1 && kwIdx + 2 < firstLine.length()) {
                String keyword = firstLine.substring(kwIdx + 2);
                reference = "<div>" + keyword + "-begin_type[" + type + "]\r\n\r\n</br>"
                        + "date: " + dateTime + "\r\n\r\n</div>\r\n"
                        + reference + "\r\n<div>" + keyword + "-end_type[" + type + "]</div>";
            } else {
                reference = "<div>" + date + "-begin_type[" + type + "]\r\n\r\n</br>"
                        + "date: " + dateTime + "\r\n\r\n</div>\r\n"
                        + reference + "\r\n<div>" + date + "-end_type[" + type + "]</div>";
            }
            reference = "\r\n\r\n" + reference;
            file = new File(oriFilePath);
        }
        try {
            if (!file.exists()) {
                log.info("ori file not exists,create one!");
                boolean newFile = file.createNewFile();
                AssertUtil.isTrue(newFile, "create new ori file failed!");
            }
            FileUtils.write(file, reference, StandardCharsets.UTF_8, !"file".equals(type));
        } catch (Exception e) {
            log.error("error occurred when write reference to orifile!", e);
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }


    private MainResponse getAndResolve(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new MainResponse();
        }
        List<String> fromLinux = getFromLinux(keyword);
//        List<String> fromLinux = new ArrayList() {
//            private static final long serialVersionUID = 1641011651395500162L;
//
//            {
//            add("_type[file]\n" +
//                    "HH\n"+
//                    "\n"+
//                    "tt\n");
//        }};
        if (CollectionUtil.isEmpty(fromLinux)) {
            return new MainResponse();
        }
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder textareaBuilder = new StringBuilder();
        fromLinux.forEach(from -> {
            String type = null;
            try {
                if (isBlank(from)) {
                    return;
                }
                //get type
                from = from.trim();
                int ridx = from.indexOf("\r");
                int nidx = from.indexOf("\n");
                int max = Math.max(ridx, nidx);
                if (max == -1) {
                    stringBuilder.append(from).append("\r\n");
                    return;
                }
                int min = Math.min(ridx, nidx);
                String firstLine = from.substring(0, min == -1 ? max : min);
                String[] sp = firstLine.split("_");
                for (String s : sp) {
                    if (s.startsWith("type[")) {
                        type = s.split("\\[")[1].split("]")[0];
                        break;
                    }
                }
                type = StringUtils.isBlank(type) ? "html" : type;
            } catch (Exception e) {
                log.error("error occurred when resolve reference!", e);
                type = "html";
            }
            log.info("type before deal-{}",type);
            if ("html".equals(type)) {
                stringBuilder.append(from).append("\r\n");
            } else if ("md".equals(type)) {
                try {
                    String[] split = from.split("</div>");
                    String endPart = MarkdownUtils.renderHtml(split[1]);
                    stringBuilder.append(split[0] + "</div>\r\n" + endPart).append("</div>\r\n");
                } catch (Exception e) {
                    log.error("error occurred when resolve md reference!", e);
                    stringBuilder.append(MarkdownUtils.renderHtml(from)).append("\r\n");
                }
            } else {
                String[] nsplit = from.split("\n");
                String[] rsplit = from.split("\r");
                ArrayList<String> lines = CollectionUtil.ofList(nsplit.length > rsplit.length ? nsplit : rsplit);
                if (CollectionUtil.isEmpty(lines)) {
                    stringBuilder.append(from).append("\r\n").toString();
                    return;
                }
                String fileNameFlagLine = lines.get(lines.size() - 1);
                if (StringUtils.contains(fileNameFlagLine, "file:")) {
                    lines.add(0, fileNameFlagLine);
                    lines.remove(lines.size() - 1);
                }
                List<String> parsedLines = new ArrayList<>();
                lines.forEach(line -> {
                    if(StringUtils.isEmpty(line)){
                        line="</br>";
                    }
                    parsedLines.add("<p style=\"margin:0px\">" + line + "</p>");
                });
                stringBuilder.append(StringUtils.join(parsedLines, "\n"));
                lines.remove(0);
                String fileFlagLine = lines.get(0);
                if(fileFlagLine.indexOf("_type[file]")!=-1){
                    lines.remove(0);
                }
                textareaBuilder.append(StringUtils.join(lines, "\n"));
            }
        });
        return new MainResponse(stringBuilder.toString(), textareaBuilder.toString());
    }

    private List<String> getFromLinux(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new ArrayList<>();
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
            return new ArrayList<>();
        }

        //get from linux
        try {
            String param = "sh " + findCommandPath + " " + keyword;
//            Process process = run.exec(new String[]{"/bin/sh", "-c", findCommandPath, keyword});
            Process process = run.exec(param);
            InputStream in = process.getInputStream();
            BufferedReader bs = new BufferedReader(new InputStreamReader(in));
            ArrayList<String> partList = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            String result = null;
            while ((result = bs.readLine()) != null) {
                if (StringUtils.equals("//delimit//", result)) {
                    stringBuilder.append("\r\n\r\n");
                    partList.add(stringBuilder.toString());
                    stringBuilder.delete(0, stringBuilder.length());
                    continue;
                }
                stringBuilder.append("\n").append(result);
            }
            in.close();
            process.destroy();
            if (stringBuilder.length() > 0) {
                partList.add(stringBuilder.toString());
            }
            List<String> reverse = CollectionUtil.reverse(partList);
            return reverse;
        } catch (IOException e) {
            log.error("error occurred when get from linux!", e);
            return new ArrayList<>();
        }
    }

    @PostMapping(value = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity upload(@RequestParam("file") MultipartFile multipartFile) {
        AssertUtil.notNull(multipartFile, "上传文件为空！");
        log.info("upload file-{}", multipartFile.getSize());
        try (InputStream inputStream = multipartFile.getInputStream();) {
            String format = new SimpleDateFormat(DateUtil.DATE_FORMAT_YMDHMS).format(new Date());
            File dir = new File(attachFileDir);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();
                AssertUtil.isTrue(mkdirs, "create attach dir failed!");
            }
            File file = new File(attachFileDir + "/" + format);
            boolean newFile = file.createNewFile();
            AssertUtil.isTrue(newFile, "create attach file failed!");
            FileUtils.copyInputStreamToFile(inputStream, file);
            return ResponseEntity.ok("/cfind/getAttach/" + format);
        } catch (Exception e) {
            log.error("error occurred when uploading file!", e);
            return ResponseEntity.status(500).body("upload failed!");
        }
    }

    @GetMapping("/getAttach/{id}")
    public ResponseEntity getAttach(@PathVariable String id, HttpServletResponse response) {
        LogUtil.apiEntry();
        File file = new File(attachFileDir + "/" + id);
        if (!file.exists()) {
            log.error("attach file not exists-{}!", id);
            return ResponseEntity.ok().build();
        }
        try (FileInputStream fileInputStream = FileUtils.openInputStream(file);
             ServletOutputStream outputStream = response.getOutputStream();) {
            IOUtils.copy(fileInputStream, outputStream);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("read attach file and write to response failed!", e);
            return ResponseEntity.ok().build();
        }
    }

    public static void main(String[] args) {
//        String ss =
//                "# markdown 航航??yy\n" +
//                        "## fdsfd\n" +
//                        "### fsdf\n" +
//                        "- fsd\n" +
//                        "> fdsf\n" +
//                        "\n" +
//                        "\n" +
//                        "##### fsdfdsfdsfs" +
//                        "<div></div>\n";
//        System.out.println(MarkdownUtils.renderHtml(ss));
        String xx = new Maincontroller().getAndResolve("xx").getTextareaContent();
        System.out.println(xx);

    }

    @Data
    public static class MainResponse {
        private String textareaContent;

        public MainResponse(String divContent, String textareaContent) {
            this.textareaContent = textareaContent;
            this.divContent = divContent;
        }

        public MainResponse() {
        }

        private String divContent;
    }
}
