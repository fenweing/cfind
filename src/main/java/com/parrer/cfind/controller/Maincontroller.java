package com.parrer.cfind.controller;

import com.parrer.cfind.MarkdownUtils;
import com.parrer.component.BaseImpl;
import com.parrer.exception.ServiceException;
import com.parrer.function.FConsumer;
import com.parrer.util.AssertUtil;
import com.parrer.util.CollcUtil;
import com.parrer.util.DateUtil;
import com.parrer.util.LogUtil;
import com.parrer.util.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trim;

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
    String htmlReq = "htmlReq";

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

    @GetMapping("/getHtml/{dir}/{srcname}")
    public ResponseEntity getHtml(@PathVariable String dir, @PathVariable String srcname, HttpServletResponse
            response) {
        LogUtil.apiEntry(dir, srcname);
        AssertUtil.isFalse(StringUtil.isBlankLeastone(dir, srcname), "dir and srcname can not be blank!");
//        srcPath = StringUtils.removeEnd(srcPath, ".html");
        File file = new File(htmlFileDir + "/" + dir + "/" + srcname);
        AssertUtil.isTrue(file.exists(), "src not exists,dir-{},srcname-{}", dir, srcname);
        try (FileInputStream fileInputStream = FileUtils.openInputStream(file);
             ServletOutputStream outputStream = response.getOutputStream();) {
            IOUtils.copy(fileInputStream, outputStream);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("read html file and write to response failed!", e);
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("/addHtml")
    public ResultResponse addReference(@Validated @RequestBody AddHtmlReq addHtmlReq) {
        addHtmlReq.setHtml(htmlSpecialHandle(addHtmlReq.getUrl(), addHtmlReq.getTitle(), addHtmlReq.getHtml()));
        ((FConsumer) getStrategyGroup(addEvent).getStrategy(htmlReq)).consume(addHtmlReq);
        return ResultResponse.ok();
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
            return ResponseEntity.ok().build();
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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        addStrategyGroup(addEvent)
                .addStrategy(html, (FConsumer<String>) (reference) -> addHtml(reference))
                .addStrategy(htmlReq, (FConsumer<AddHtmlReq>) (addHtmlReq) -> dealHtml(addHtmlReq));
    }

    private void addHtml(String reference) {
        AssertUtil.notEmpty(reference, "html content can not be blank!");
        //deal kw line
        String firstLine = reference.substring(0, reference.indexOf("\n"));
        int kwIdx = firstLine.indexOf("??");
        String keyword = firstLine.substring(kwIdx + 2);
        String logReference = reference.replace("$", EMPTY).replace("{", EMPTY);
        AssertUtil.notEmpty(keyword, "key word is null,reference-{}", logReference);
        //deal kw line end
        //deal host line
        reference = reference.substring(firstLine.length() + 1);
        int hostEndIdx = reference.indexOf("\n");
        AssertUtil.isTrue(hostEndIdx != -1, "invalid html content-{}", logReference);
        String hostLine = reference.substring(0, hostEndIdx);
        String prefix = hostLine.startsWith("http://") ? "http://" :
                (hostLine.startsWith("https://") ? "https://" : "");
        AssertUtil.notEmpty(prefix, "invalid html content-{}", logReference);
        String substring = hostLine.substring(prefix.length());
        int sepIdx = substring.indexOf("/");
        String host = sepIdx == -1 ? substring : substring.substring(0, sepIdx);
        AssertUtil.notEmpty(host, "invalid html content-{}", logReference);
        host = prefix + host + "/";
        //deal host line end
        String htmlReference = reference.substring(hostLine.length());
        AssertUtil.notEmpty(htmlReference, "empty html reference,total reference-{}", logReference);
        //special condition handle
        htmlReference = htmlSpecialHandle(host, keyword, htmlReference);
        //special condition handle end
        //deal head block
        String completeHtml = EMPTY;
        if (htmlReference.startsWith("<htm")) {
            log.info("html literal mode");
            completeHtml = htmlReference;
        } else if (htmlReference.startsWith("<head")) {
            log.info("head mode");
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
        } else if (htmlReference.startsWith("<div")) {
            log.info("html and div mode");
            completeHtml = getHtmlFromUrlAndDiv(hostLine, htmlReference);
        }
        dealHtml(AddHtmlReq.of().withHtml(completeHtml).withUrl(host).withTitle(keyword));
    }

    private String htmlSpecialHandle(String host, String keyword, String htmlReference) {
        if (StringUtil.isBlankLeastone(keyword, host, htmlReference)) {
            return htmlReference;
        }
        if (host.toLowerCase().contains("csdn")) {
            htmlReference = htmlReference.replace("window.location.href", "");
        }
        return removeNextLineLabel(htmlReference);
    }

    private void dealHtml(AddHtmlReq addHtmlReq) {
        String keyword = addHtmlReq.getTitle();
        String host = addHtmlReq.getUrl();
        String completeHtml = addHtmlReq.getHtml();
        AssertUtil.notEmpty(completeHtml, "html text is blank after resolve!");
        String dateFlag = new SimpleDateFormat(DateUtil.DATE_FORMAT_YMDHMS).format(new Date());
        String dirName = keyword + "_" + dateFlag;
        File htmlDir = new File(this.htmlFileDir + "/" + dirName);
        try {
            FileUtils.forceMkdir(htmlDir);
        } catch (IOException e) {
            log.error("mkdir failed!-{}", dirName, e);
            throw new ServiceException(e, "mkdir failed!");
        }
        completeHtml = dealScriptItem(completeHtml);
        completeHtml = dealLinkItem(completeHtml, host, dirName);
        completeHtml = dealImgItem(completeHtml, host, dirName);
        completeHtml = dealOther(completeHtml);
        boolean writeHtmlFile = createAndWriteFile(completeHtml, new File(htmlFileDir + "/" + dirName + "/" + dirName + ".html"), false);
        AssertUtil.isTrue(writeHtmlFile, "create html file failed!");
    }

    private String dealImgItem(String completeHtml, String host, String dirName) {
        AssertUtil.notEmpty(completeHtml, "html string can not be blank!");
        AssertUtil.notEmpty(host, "host can not be blank!");
        Document doc = Jsoup.parse(completeHtml);
        Elements imgs = doc.getElementsByTag("img");
        if (imgs.size() == 0) {
            return completeHtml;
        }
        for (Element img : imgs) {
            try {
                String src = img.attr("src");
                if (isBlank(src)) {
                    continue;
                }
                src = startsWith(src, "http") ? src : (host + removeStart(removeStart(src, "/"), "/"));//存在这种形式的地址：//uri/path
                String imgName = HtmlUtil.getUrlSourceName(src);
                File file = new File(htmlFileDir + "/" + dirName + "/" + imgName);
                if (!file.exists()) {
                    boolean newFile = file.createNewFile();
                    AssertUtil.isTrue(newFile, "create new img file failed!");
                }
                com.parrer.util.HttpUtil.requestFile(src, file.getPath());
                img.attr("src", "/cfind/getHtml/" + dirName + "/" + imgName);
            } catch (Exception e) {
                log.error("deal img element failed!,link-{}", img.html());
            }
        }
        return doc.html();
    }

    private String removeNextLineLabel(String htmlReference) {
        if (isBlank(htmlReference)) {
            return htmlReference;
        }
        while (startsWith(htmlReference, "\r") || startsWith(htmlReference, "\n")
                || endsWith(htmlReference, "\r") || endsWith(htmlReference, "\n")) {
            htmlReference = removeStart(htmlReference, "\r");
            htmlReference = removeStart(htmlReference, "\n");
            htmlReference = removeEnd(htmlReference, "\r");
            htmlReference = removeEnd(htmlReference, "\n");
        }
        return htmlReference;
    }

    private String getHtmlFromUrlAndDiv(String hostLine, String htmlReference) {
        AssertUtil.notEmpty(StringUtil.isBlankLeastone(hostLine, htmlReference), "html url and div reference can not be blank!");
        Map<String, String> resMap = HttpUtil.init().get(hostLine);
        AssertUtil.isTrue("200".equals(resMap.get("statusCode")), "request html failed,return code-{}", resMap.get("statusCode"));
        String result = resMap.get("result");
        AssertUtil.notEmpty(result, "request html return blank!");
        Document doc = Jsoup.parse(result);
        Elements links = doc.getElementsByTag("link");
        String linkJoin = join(links, EMPTY);
        Elements styles = doc.getElementsByTag("style");
        String styleJoin = join(styles, EMPTY);
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">")
                .append(linkJoin).append(styleJoin).append("</head><body>")
                .append(htmlReference).append("</body></html>");
        return htmlBuilder.toString();
    }

    private String dealOther(String completeHtml) {
        return completeHtml;
    }

    private String dealLinkItem(String completeHtml, String host, String dirName) {
        AssertUtil.notEmpty(completeHtml, "html string can not be blank!");
        AssertUtil.notEmpty(host, "host can not be blank!");
        Document doc = Jsoup.parse(completeHtml);
        Elements links = doc.getElementsByTag("link");
        if (links.size() == 0) {
            return completeHtml;
        }
        HttpUtil httpUtil = HttpUtil.init();
        for (Element link : links) {
            try {
                String href = link.attr("href");
                if (isBlank(href)) {
                    link.remove();
                    continue;
                }
                href = startsWith(href, "http") ? href : (host + removeStart(removeStart(href, "/"), "/"));//存在这种形式的地址：//uri/path
                String cssName = HtmlUtil.getUrlSourceName(href);
                if (!endsWith(cssName, ".css")) {
                    log.error("not css file!-{}", cssName);
                    continue;
                }
//                boolean css = "stylesheet".equals(link.attr("rel")) ||
//                        "text/css".equals(link.attr("type"));
//                if (!css) {
//                    link.remove();
//                    continue;
//                }
                Map<String, String> resMap = httpUtil.get(href);
                if (!"200".equals(resMap.get("statusCode"))) {
                    log.error("request css link failed!,href-{}", href);
                    continue;
                }
                String result = resMap.get("result");
                if (isBlank(result)) {
                    log.error("request css link return blank!,href-{}", href);
                    continue;
                }
                boolean writeFile = createAndWriteFile(result, new File(htmlFileDir + "/" + dirName + "/" + cssName), false);
                if (!writeFile) {
                    log.error("write css text to file failed!-{}", cssName);
                    continue;
                }
                link.attr("href", "/cfind/getHtml/" + dirName + "/" + cssName);
            } catch (Exception e) {
                log.error("deal link element failed!,link-{}", link.html());
            }
        }
        return doc.html();
    }

    private String dealScriptItem(String completeHtml) {
        AssertUtil.notEmpty(completeHtml, "html string can not be blank!");
        Document doc = Jsoup.parse(completeHtml);
        Elements script = doc.getElementsByTag("script");
        script.remove();
//        log.info(doc.html());
        //todo del script in iframe
        return doc.html();
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
//                add("htmldir: jsoup修改html属性_20210906140659\r\n");
//            }
//        };
        if (CollectionUtils.isEmpty(fromLinux)) {
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
                //html file
                String htmldirFlag = "htmldir:";
                if (from.startsWith(htmldirFlag)) {
                    String dirs = trim(from.substring(htmldirFlag.length()));
                    if (isBlank(dirs)) {
                        return;
                    }
                    String[] disArr = dirs.split(" ");
                    mainResponse.getHtmldir().addAll(CollcUtil.ofList(disArr));
                    return;
                }
                int ridx = from.indexOf("\r");
                int nidx = from.indexOf("\n");
                int max = Math.max(ridx, nidx);
                if (max == -1) {
                    stringBuilder.append(from).append("\r\n");
                    return;
                }
                int min = Math.min(ridx, nidx);
                String firstLine = from.substring(0, min == -1 ? max : min);
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
                ArrayList<String> lines = CollcUtil.ofList(nsplit.length > rsplit.length ? nsplit : rsplit);
                if (CollcUtil.isEmpty(lines)) {
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
            List<String> reverse = CollcUtil.reverse(partList);
            return reverse;
        } catch (IOException e) {
            log.error("error occurred when get from linux!", e);
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Maincontroller maincontroller = new Maincontroller();
//        MainResponse andResolve = new Maincontroller().getAndResolve("xx");
//        System.out.println(JsonUtil.toString(andResolve));
//        String ss = null;
//        try {
//            ss = FileUtils.readFileToString(new File("D:\\virtualBox\\shared\\mini.html"), StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String xx = new Maincontroller().dealScriptItem(ss);
//        System.out.println(xx);
        Map<String, String> resMap = HttpUtil.init().get("https://www.jianshu.com/p/2425a1c14755");
        String result = resMap.get("result");
//        log.info(result);
        maincontroller.addHtml("??ii3\n" +
                "https://www.jianshu.com/p/2425a1c14755\n" +
                "<div ");
//        try {
//            FileUtils.write(new File("d:\\\\tt.html"), result);
//    } catch (IOException e) {
//        e.printStackTrace();
//    }
//        System.out.println(startsWith());
    }

    @Data
    public static class MainResponse {
        private String textareaContent;
        private String divContent;
        private List<String> htmldir = new ArrayList<>();


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
