/*
 * 
 * Copyright 2014 Michael Prentice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import retrofit.client.Response;
import retrofit.http.Multipart;
import retrofit.http.Part;
import retrofit.http.Streaming;
import retrofit.mime.TypedFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * You will need to create one or more Spring controllers to fulfill the
 * requirements of the assignment.
 */
@Controller
public class VideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);

    private Map<Long,Video> videos = new HashMap<Long, Video>();

    public Video save(Video entity) {
        checkAndSetId(entity);
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if (entity.getId() == 0) {
            entity.setId(currentId.incrementAndGet());
        }
    }

    @RequestMapping(value = "/video", method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<Video> getVideoList() {
        return videos.values();
    }

    @RequestMapping(value = "/video", method = RequestMethod.POST)
    public
    @ResponseBody
    Video addVideo(@RequestBody Video v) {
        v.setDataUrl(getUrlBaseForLocalServer() + "/video/" + v.getId() + "/data");
        return save(v);
    }

	@Streaming
    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
    public
    @ResponseBody
    void getData(@PathVariable("id") long id, HttpServletResponse response) {
        Video video = videos.get(id);

        if (video == null) {
            throw new ResourceNotFoundException("Video not found for id: " + String.valueOf(id));
        }

        try {
            if (!VideoFileManager.get().hasVideoData(video)) {
                throw new ResourceNotFoundException("Video data not found for id: " + String.valueOf(id));
            }

            OutputStream outputStream = response.getOutputStream();
            VideoFileManager.get().copyVideoData(video, outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@Multipart
    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
    public
    @ResponseBody
    VideoStatus setVideoData(@PathVariable("id") long id, @RequestParam("data") MultipartFile videoData) {
        Video video = videos.get(id);
        if (video == null) {
            throw new ResourceNotFoundException("Video not found for id: " + String.valueOf(id));
        }

        try {
            InputStream inputStream = videoData.getInputStream();
            VideoFileManager.get().saveVideoData(video, inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new VideoStatus(VideoStatus.VideoState.READY);
    }


    private String getUrlBaseForLocalServer() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return "http://" + request.getServerName() + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
    }
}
