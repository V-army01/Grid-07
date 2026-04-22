package com.grid07.controller;

import com.grid07.dto.CreateCommentRequest;
import com.grid07.dto.CreatePostRequest;
import com.grid07.dto.LikeRequest;
import com.grid07.entity.Comment;
import com.grid07.entity.Post;
import com.grid07.service.CommentService;
import com.grid07.service.PostService;
import com.grid07.service.RedisViralityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService          postService;
    private final CommentService       commentService;
    private final RedisViralityService viralityService;

    /** POST /api/posts */
    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody CreatePostRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(req));
    }

    /** POST /api/posts/{postId}/comments */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(postId, req));
    }

    /** POST /api/posts/{postId}/like */
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> likePost(
            @PathVariable Long postId,
            @Valid @RequestBody LikeRequest req) {
        postService.likePost(postId, req.getUserId());
        return ResponseEntity.ok(Map.of(
                "postId",        postId,
                "viralityScore", viralityService.getViralityScore(postId)
        ));
    }

    /** GET /api/posts/{postId}/virality */
    @GetMapping("/{postId}/virality")
    public ResponseEntity<Map<String, Object>> getVirality(@PathVariable Long postId) {
        return ResponseEntity.ok(Map.of(
                "postId",        postId,
                "viralityScore", viralityService.getViralityScore(postId),
                "botCount",      viralityService.getBotCount(postId)
        ));
    }
}