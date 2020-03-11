package com.wizzstudio.githubproxy.controller;

import com.upyun.MediaHandler;
import com.upyun.PullingHandler;
import com.upyun.Result;
import com.upyun.UpException;
import com.wizzstudio.githubproxy.Record;
import com.wizzstudio.githubproxy.RecordRepository;
import com.wizzstudio.githubproxy.TimeHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/record")
@CrossOrigin
public class RecordController {
    public final RecordRepository recordRepository;

    public RecordController(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Value("${up.bucket}")
    private String bucket;
    @Value("${up.operator.name}")
    private String operatorName;
    @Value("${up.operator.password}")
    private String operatorPassword;
    @Value("${up.callbackURL}")
    private String callbackURL;

    private String upYunRemoteDownload(String url) throws Exception {

        MediaHandler handler = new MediaHandler(bucket, operatorName, operatorPassword);

        Map<String, Object> paramsMap = new HashMap<>();

        paramsMap.put(PullingHandler.Params.BUCKET_NAME, bucket);
        paramsMap.put(PullingHandler.Params.NOTIFY_URL, callbackURL);//todo host
        paramsMap.put(PullingHandler.Params.APP_NAME, "spiderman");

        JSONArray array = new JSONArray();

        JSONObject json = new JSONObject();
        json.put(PullingHandler.Params.URL, url);

        var strings = url.split("/");
        var filename = strings[strings.length - 1];
        if (url.contains("github.com") && filename.equals("master.zip")) {
            filename = strings[strings.length - 3] + "-" + strings[strings.length - 1];
        }
        //https://github.com/pawelsalawa/sqlitestudio/archive/master.zip
        //->
        //sqlitestudio-master.zip

        System.out.println(filename);

        json.put(PullingHandler.Params.SAVE_AS, filename);

        array.put(json);

        paramsMap.put(PullingHandler.Params.TASKS, array);

        try {
            Result result = handler.process(paramsMap);
            System.out.println(result);
            if (result.isSucceed()) {
                String arrayStr = Arrays.toString(handler.getTaskId(result.getMsg()));
                System.out.println(arrayStr);
                return arrayStr.substring(1, arrayStr.length() - 1);
            } else {
                throw new Exception("create task failed");
            }

        } catch (IOException | UpException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping(path = "", produces = "application/json")
    public @ResponseBody
    Iterable<Record> getAllRecords() {
        return recordRepository.findAll();
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public @ResponseBody
    Optional<Record> getRecord(@PathVariable int id) {
        return recordRepository.findById(id);
    }

    @DeleteMapping(path = "", produces = "application/json")
    public @ResponseBody
    String deleteAllRecords() {
        recordRepository.deleteAll();
        return "\"delete ok\"";
    }

    @PostMapping(path = "", produces = "application/json")
    public @ResponseBody
    ResponseEntity<?> createRecord(@RequestBody String url) throws Exception {
        if (!url.contains("https://github.com/")) {
            return new ResponseEntity<>("{\"message\":\"Not contains github\"}", HttpStatus.BAD_REQUEST);
        }
        if (url.contains(".git")) {
            url = url.replace(".git", "/archive/master.zip");
        }

        System.out.println(url);

        System.out.println(bucket);
        System.out.println(operatorName);
        System.out.println(operatorPassword);

        String taskID = upYunRemoteDownload(url);
        Record record = new Record();
        record.setRequestUrl(url);
        record.setTaskID(taskID);
        recordRepository.save(record);
        return new ResponseEntity<>(record, HttpStatus.OK);
    }

    @PostMapping(path = "/callback", produces = "application/json")
    public @ResponseBody
    String callback(@RequestBody String body) {
        //{"bucket_name":"githubproxy","path":"/2020-02-20;12-03-04;master.zip","status_code":200,"task_id":"987711448be94336a7516efdec5010a2"}
        System.out.println(body);
        JSONObject b = new JSONObject(body);
        String taskId = b.getString("task_id");
        String path = b.getString("path");
        var records = recordRepository.findAll();
        for (var record : records) {
            if (record.getTaskID().equals(taskId)) {
                record.setFileUrl("http://githubproxy.test.upcdn.net" + path);
                recordRepository.save(record);
                break;
            }
        }
        return "";
    }
}
