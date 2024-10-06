package com.kogo.content.service

import com.kogo.content.endpoint.model.PaginationRequest
import com.kogo.content.endpoint.model.PaginationResponse
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.repository.PostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit


@Service
class FeedService(
    private val postRepository: PostRepository,
) {
    fun listFeedsByLatest(paginationRequest: PaginationRequest): PaginationResponse<Post> {
        val limit = paginationRequest.limit
        val page = paginationRequest.page
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val posts = if (page != null) {
            postRepository.findAllByIdLessThanOrderByIdDesc(page, pageable)
        } else {
            postRepository.findAllByOrderByIdDesc(pageable)
        }
        val nextPageToken = posts.lastOrNull()?.id
        return PaginationResponse(posts, nextPageToken)
    }

    fun listFeedsByPopular(paginationRequest: PaginationRequest): PaginationResponse<Post> {
        val limit = paginationRequest.limit
        val page = paginationRequest.page
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val withinTwoWeeks = Instant.now().minus(14, ChronoUnit.DAYS)
        val posts = if (page != null) {
            postRepository.findAllByIdLessThanAndViewcountGreaterThanAndCreatedAtAfter(page, 100, withinTwoWeeks, pageable)
        } else {
            postRepository.findAllByViewcountGreaterThanAndCreatedAtAfter(100, withinTwoWeeks, pageable)
        }
        val sortedPosts = posts.sortedByDescending { calculateScore(it) }

        val nextPageToken = sortedPosts.lastOrNull()?.id
        return PaginationResponse(sortedPosts, nextPageToken)
    }

    private fun calculateScore(post: Post): Double {
        return (post.likes.toDouble() / post.viewcount) + (post.commentCount * 0.01)
    }
}
