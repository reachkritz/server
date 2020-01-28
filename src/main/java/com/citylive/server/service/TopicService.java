package com.citylive.server.service;

import com.citylive.server.MTree.Planar.MTree2D;
import com.citylive.server.MTree.common.Data;
import com.citylive.server.dao.TopicRepository;
import com.citylive.server.dao.UserRepository;
import com.citylive.server.dao.UserTopicRepository;
import com.citylive.server.domain.Topic;
import com.citylive.server.domain.UserTopic;
import com.citylive.server.domain.UserTopicPK;
import com.citylive.server.pojo.MessageType;
import com.citylive.server.pojo.Query;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopicService {
    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTopicRepository userTopicRepository;

    @Autowired
    MessagingService messagingService;
    @Autowired
    MTree2D mtree;

    public Topic addTopic(Topic topic){

        Topic topic1 = topicRepository.save(topic.toBuilder().closed(false).time(new Timestamp((new Date()).getTime())).build());

        List<String> nearbyUsers = mtree
                .getNearestAsList(new Data(topic.getUserName(),topic.getLongitude(),topic.getLatitude()))
                .stream()
                .map(rs->rs.data.getId())
                .collect(Collectors.toList());

        Query query = new Query();
        query.setDeviceIds(
                nearbyUsers.stream()
                        .map(userId->userRepository.getDeviceIdForUserId(userId))
                        .collect(Collectors.toList())
        );
        query.setQuestion(topic.getQuestion());
        query.setTopic(String.valueOf(topic.getTopicId()));
        query.setSender(topic.getUserName());
        query.setSenderDeviceId(userRepository.getDeviceIdForUserId(topic.getUserName()));
        query.setType(MessageType.QUESTION);

        try {
            messagingService.sendNotificationToMultipleDevices(query);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }

        return topic1;
    }

    public List<Topic> getTopicByUser(final String userName){
        return topicRepository.getTopicByUser(userName);
    }

    public void subscribeTopic(final String userName, final Integer topicId){
        if(!userTopicRepository.existsById(UserTopicPK.builder().topicId(topicId).userName(userName).build())){
            userTopicRepository.save(UserTopic.builder().topicId(topicId).userName(userName).build());
        }
    }

    public void unsubscribeTopic(final String userName, final Integer topicId){
        if(userTopicRepository.existsById(UserTopicPK.builder().topicId(topicId).userName(userName).build())){
            userTopicRepository.delete(UserTopic.builder().topicId(topicId).userName(userName).build());
        }
    }

    public List<Topic> getTopicSubscribedByUser(String userName) {
        List<UserTopic> topicSubscribeByUser = userTopicRepository.getTopicSubscribeByUser(userName);
        final Set<Integer> topicIds = topicSubscribeByUser.stream().map(t -> t.getTopicId()).collect(Collectors.toSet());
        return topicIds.isEmpty() ? new ArrayList<>() : topicRepository.getTopicByIds(topicIds);
    }
}