package com.parrer.cfind.controller;

import com.parrer.cfind.MarkdownUtils;
import com.parrer.component.BaseImpl;
import com.parrer.function.FConsumer;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
public class Maincontroller extends BaseImpl implements ApplicationRunner {
    @Value("${cfind.command:/cfind/cfind.sh}")
    private String findCommandPath;
    @Value("${cfind.oriFilePath:/cfind/file}")
    private String oriFilePath;
    @Value("${cfind.attachFileDir:/cfind/attach}")
    private String attachFileDir;
    @Value("${cfind.docFileDir:/cfind/doc}")
    private String docFileDir;
    @Value("${cfind.htmlFileDir:/cfind/html}")
    private String htmlFileDir;
    String addEvent = "addEvent";
    String html = "html";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        addStrategyGroup(addEvent).addStrategy(html, (FConsumer<String>) (reference) -> {
            addHtml(reference);
        });
    }

    private void addHtml(String reference) {
        AssertUtil.notEmpty(reference, "html content can not be blank!");
        //deal kw line
        String firstLine = reference.substring(0, reference.indexOf("\n"));
        int kwIdx = firstLine.indexOf("??");
        String keyword = firstLine.substring(kwIdx + 2);
        AssertUtil.notEmpty(keyword, "key word is null,reference-{}", reference);
        //deal kw line end
        //deal host line
        reference = reference.substring(firstLine.length());
        int hostEndIdx = reference.indexOf("\n");
        AssertUtil.isTrue(hostEndIdx != -1, "invalid html content-{}", reference);
        String hostLine = reference.substring(0, hostEndIdx);
        String prefix = hostLine.startsWith("http://") ? "http://" :
                (hostLine.startsWith("https://") ? "https://" : "");
        AssertUtil.notEmpty(prefix, "invalid html content-{}", reference);
        String substring = hostLine.substring(prefix.length());
        int sepIdx = substring.indexOf("/");
        String host = sepIdx == -1 ? substring : substring.substring(0, sepIdx);
        AssertUtil.notEmpty(host, "invalid html content-{}", reference);
        host = prefix + host + "/";
        //deal host line end
        //deal head block
        String htmlReference = reference.substring(hostLine.length());
        AssertUtil.notEmpty(htmlReference, "empty html reference,total reference-{}", reference);
        String completeHtml = htmlReference;
        if (!htmlReference.startsWith("<html>")) {
            int headBeginIdx = htmlReference.indexOf("<head>");
            int headEndIdx = htmlReference.indexOf("</head>");
            String headBlock = "";
            if (headBeginIdx != -1 && headEndIdx != -1) {
                headBlock = htmlReference.substring(headBeginIdx, headEndIdx + 7);
            }
            //deal head block end
            //deal div block
            String divBlock = headEndIdx != -1 ? htmlReference.substring(headEndIdx + 8) : htmlReference;
            AssertUtil.notEmpty(divBlock, "divBlock can not be null!");
            //deal div block end
            completeHtml = "<html>" + headBlock + "<body>" + divBlock + "</body></html>";
        }
        String dateFlag = new SimpleDateFormat(DateUtil.DATE_FORMAT_YMDHMS).format(new Date());
        completeHtml = dealScriptItem(completeHtml);
        String dirName = keyword + "_" + dateFlag;
        completeHtml = dealLinkItem(completeHtml, dirName);
        boolean writeHtmlFile = createAndWriteFile(completeHtml, new File(htmlFileDir + "/" + dirName + "/" + dirName + ".html"), false);
        AssertUtil.isTrue(writeHtmlFile, "create html file failed!");
    }

    @GetMapping("/search/{keyword}")
    public ResponseEntity getByKeyword(@PathVariable("keyword") String keyword) {
        LogUtil.apiEntry(keyword);
        if (StringUtils.isBlank(keyword)) {
            log.error("blank keyword!");
            return ResponseEntity.ok().build();
        }
//        html=MarkdownUtils.renderHtml(html);
        MainResponse response = getAndResolve(keyword);
//        String html="<div><img src=\"/cfind/getAttach/20210828182837\"></div>";
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getHtml/{page}")
    public ResponseEntity getHtml(@PathVariable String page, HttpServletResponse response) {
        LogUtil.apiEntry(page);
        AssertUtil.notEmpty(page, "get html dir can not be blank!");
        page = StringUtils.removeEnd(page, ".html");
        File file = new File(htmlFileDir + "/" + page + "/" + page);
        AssertUtil.isTrue(file.exists(), "html dir not exists,dir-{}", page);
        try (FileInputStream fileInputStream = FileUtils.openInputStream(file);
             ServletOutputStream outputStream = response.getOutputStream();) {
            IOUtils.copy(fileInputStream, outputStream);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("read html file and write to response failed!", e);
            return ResponseEntity.ok().build();
        }
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
            reference = "<p>_type[file]</p>\r\n" + reference;
        } else if ("html".equals(type)) {
            ((FConsumer) getStrategyGroup(addEvent).getStrategy(html)).consume(reference);
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
        if (!createAndWriteFile(reference, file, !"file".equals(type))) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }

    private boolean createAndWriteFile(String reference, File file, boolean append) {
        try {
            if (!file.exists()) {
                log.info("ori file not exists,create one!");
                boolean newFile = file.createNewFile();
                AssertUtil.isTrue(newFile, "create new ori file failed!");
            }
            FileUtils.write(file, reference, StandardCharsets.UTF_8, append);
            return true;
        } catch (Exception e) {
            log.error("error occurred when write reference to orifile!", e);
            return false;
        }
    }


    private MainResponse getAndResolve(String keyword) {
        MainResponse mainResponse = new MainResponse();
        if (StringUtils.isBlank(keyword)) {
            return mainResponse;
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
            return mainResponse;
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
                //html file
                String htmldirFlag = "htmldir:";
                if (firstLine.startsWith(htmldirFlag)) {
                    String dirs = firstLine.substring(htmldirFlag.length());
                    if (isBlank(dirs)) {
                        return;
                    }
                    String[] disArr = dirs.split(" ");
                    mainResponse.getHtmldir().addAll(CollectionUtil.ofList(disArr));
                    return;
                }
                //html file end
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
            log.info("type before deal-{}", type);
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
                    if (StringUtils.isEmpty(line)) {
                        line = "</br>";
                    }
                    parsedLines.add("<p style=\"margin:0px\">" + line + "</p>");
                });
                stringBuilder.append(StringUtils.join(parsedLines, "\n"));
                lines.remove(0);
                String fileFlagLine = lines.get(0);
                if (fileFlagLine.indexOf("_type[file]") != -1) {
                    lines.remove(0);
                }
                textareaBuilder.append(StringUtils.join(lines, "\n"));
            }
        });
        return mainResponse.setDivContent(stringBuilder.toString())
                .setTextareaContent(textareaBuilder.toString());
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
        LogUtil.apiEntry(id);
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
        private String divContent;
        private List<String> htmldir;


        public MainResponse(String divContent, String textareaContent) {
            this.textareaContent = textareaContent;
            this.divContent = divContent;
        }

        public MainResponse() {
        }

        public MainResponse setTextareaContent(String textareaContent) {
            this.textareaContent = textareaContent;
            return this;
        }

        public MainResponse setDivContent(String divContent) {
            this.divContent = divContent;
            return this;
        }

        public MainResponse setHtmldir(List<String> htmldir) {
            this.htmldir = htmldir;
            return this;
        }
    }
}
