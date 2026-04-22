package com.grid07.service;

import com.grid07.dto.CreatePostRequest;
import com.grid07.entity.Post;
import com.grid07.repository.BotRepository;
import com.grid07.repository.PostRepository;
import com.grid07.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository       postRepository;
    private final UserRepository       userRepository;
    private final BotRepository        botRepository;
    private final RedisViralityService viralityService;
    private final StringRedisTemplate  redis;

    @Transactional
    public Post createPost(CreatePostRequest req) {
        if (req.getAuthorType() == Post.AuthorType.USER) {
            userRepository.findById(req.getAuthorId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + req.getAuthorId()));
        } else {
            botRepository.findById(req.getAuthorId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot not found: " + req.getAuthorId()));
        }

        return postRepository.save(Post.builder()
                .authorId(req.getAuthorId())
                .authorType(req.getAuthorType())
                .content(req.getContent())
                .build());
    }

    @Transactional
    public void likePost(Long postId, Long userId) {
        postRepository.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId));
        userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        String  likeKey = "post:" + postId + ":liked_by:" + userId;
        Boolean isNew   = redis.opsForValue().setIfAbsent(likeKey, "1");
        if (Boolean.TRUE.equals(isNew)) {
            viralityService.incrementOnHumanLike(postId);
            log.info("User {} liked post {}. Virality: {}", userId, postId,viralityService.getViralityScore(postId));
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User " + userId + " has already liked post " + postId);
        }
    }

    public Post getPost(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId));
    }
}
