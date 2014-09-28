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
package org.magnum.mobilecloud.video;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

@Controller
public class VideoController {

	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to do the injection.
	//
	@Autowired
	private VideoRepository videos;

    private Video getVideo(long id) {
        Video video = videos.findOne(id);
        if (video == null) {
            throw new ResourceNotFoundException("Video not found for id: " + String.valueOf(id));
        }
        return video;
    }

//	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
//	public Video addVideo(@Body Video v) {
//        System.out.println("Adding video " + v.getName());
//        return videos.save(v);
//    }

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method = RequestMethod.POST)
    public Void likeVideo(@PathVariable("id") long id, HttpServletRequest request) {
        Video video = getVideo(id);
        video.setLikes(video.getLikes() + 1L);
        video.addLikedby(request.getRemoteUser());
        videos.save(video);
        return null;
    }

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method = RequestMethod.POST)
    public Void unlikeVideo(@PathVariable("id") long id, HttpServletRequest request) {
        Video video = getVideo(id);
        video.setLikes(video.getLikes() - 1L);
        video.removeLikedby(request.getRemoteUser());
        videos.save(video);
        return null;
    }

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method = RequestMethod.GET)
    public Collection<String> getUsersWhoLikedVideo(@PathVariable("id") long id) {
        Video video = getVideo(id);
        return video.getLikedby();
    }
}
