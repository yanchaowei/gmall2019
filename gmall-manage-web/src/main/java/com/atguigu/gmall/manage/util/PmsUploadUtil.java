package com.atguigu.gmall.manage.util;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {



    //上传图片，返回图片存储地址的String
    public static String uploadImage(MultipartFile multipartFile) {

        String imgUrl = "http://172.19.240.60";

        //配置fdfs的全局连接地址
        String tracker = PmsUploadUtil.class.getResource("/tracker.conf").getPath();

        try {
            ClientGlobal.init(tracker);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();

        //获取trackerServer的一个实例
        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //通过tracker获得一个storage
        StorageClient storageClient = new StorageClient(trackerServer, null);

        try {
            //获得上传的二进制对象
            byte[] bytes = multipartFile.getBytes();

            //获得文件后缀名
            //根据二进制对象获得文件名
            String fileName = multipartFile.getOriginalFilename();
            int x = fileName.lastIndexOf(".");
            String extName = fileName.substring(x + 1);

            String[] uploadInfos = storageClient.upload_file(bytes, extName, null);

            for (String uploadInfo : uploadInfos) {
                imgUrl += "/" + uploadInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imgUrl;
    }

}
