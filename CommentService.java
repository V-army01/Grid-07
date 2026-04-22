package com.grid07.service;

import com.grid07.dto.CreateCommentRequest;
import com.grid07.entity.Bot;
import com.grid07.entity.Comment;
import com.grid07.entity.Post;
import com.grid07.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository    commentRepository;
    private final PostRepository       postRepository;
    private final UserRepository       userRepository;
    private final BotRepository        botRepository;
    private final RedisViralityService viralityService;
    private final NotificationService  notificationService;

    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest req) {

        // 1. Post must exist
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Post not found: " + postId));

        // 2. Compute depth from parent
        int depth = 0;
        if (req.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(req.getParentCommentId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Parent comment not found"));
            depth = parent.getDepthLevel() + 1;
        }

        // ── BOT path
        if (req.getAuthorType() == Comment.AuthorType.BOT) {
            Bot bot = botRepository.findById(req.getAuthorId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Bot not found: " + req.getAuthorId()));
            if (!viralityService.isDepthAllowed(depth)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Vertical cap exceeded: thread depth > 20");
            }
            if (!viralityService.tryIncrementBotCount(postId)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Horizontal cap exceeded: post already has 100 bot replies");
            }
            if (post.getAuthorType() == Post.AuthorType.USER) {
                Long humanId = post.getAuthorId();
                if (!viralityService.tryAcquireCooldown(bot.getId(), humanId)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Cooldown active: bot cannot interact with this user within 10 minutes");
                }

                try {
                    Comment comment = persist(postId, req, depth);
                    viralityService.incrementOnBotReply(postId);
                    notificationService.handleBotInteractionNotification(
                        humanId, bot.getId(), bot.getName(), "replied to your post");
                    return comment;
                } catch (Exception e) {
                   
                    viralityService.decrementBotCount(postId);
                    log.error("DB save failed for bot comment on post {}. Redis compensated.", postId);
                    throw e;
                }
            }

            try {
                Comment comment = persist(postId, req, depth);
                viralityService.incrementOnBotReply(postId);
                return comment;
            } catch (Exception e) {
                viralityService.decrementBotCount(postId);
                log.error("DB save failed for bot comment on post {}. Redis compensated.", postId);
                throw e;
            }
        }

        // ── HUMAN path 
        userRepository.findById(req.getAuthorId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found: " + req.getAuthorId()));

        Comment comment = persist(postId, req, depth);
        viralityService.incrementOnHumanComment(postId);
        return comment;
    }

    private Comment persist(Long postId, CreateCommentRequest req, int depth) {
        return commentRepository.save(Comment.builder()
                .postId(postId)
                .authorId(req.getAuthorId())
                .authorType(req.getAuthorType())
                .content(req.getContent())
                .depthLevel(depth)
                .parentCommentId(req.getParentCommentId())
                .build());
    }
}
