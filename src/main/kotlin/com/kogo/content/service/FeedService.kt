package com.kogo.content.service

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationSlice
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.repository.PostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service


@Service
class FeedService(
    private val postRepository: PostRepository,
) {
    fun getAllPostsByLatest(paginationRequest: PaginationRequest): PaginationSlice<Post> {
        val limit = paginationRequest.limit
        val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val posts = if (pageLastResourceId != null) postRepository.findAllByIdLessThanOrderByIdDesc(pageLastResourceId, pageable)
                    else postRepository.findAllByOrderByIdDesc(pageable)

        val nextPageToken = posts.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
        return PaginationSlice(posts, nextPageToken)
    }

    fun getAllPostsByPopularity(paginationRequest: PaginationRequest): PaginationSlice<Post> {
        val limit = paginationRequest.limit
        val pageNumber = paginationRequest.pageToken.pageLastResourceId ?: "0"
        val pageable = PageRequest.of(pageNumber.toInt(), limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable

        val posts = postRepository.findAllPopular(pageable.pageNumber, pageable.pageSize)
        val nextPageNumber = paginationRequest.pageToken.nextPageToken((pageNumber.toInt() + 1).toString())
        return PaginationSlice(posts, nextPageNumber)
    }
}
