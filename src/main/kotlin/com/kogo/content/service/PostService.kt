package com.kogo.content.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.endpoint.model.PaginationRequest
import com.kogo.content.endpoint.model.PaginationResponse
import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.searchengine.*
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class PostService (
    private val repository: PostRepository,
    private val attachmentRepository: AttachmentRepository,
    private val likeRepository: LikeRepository,
    private val viewRepository: ViewRepository,
    private val fileHandler: FileHandler,
) {
    fun find(postId: String): Post? = repository.findByIdOrNull(postId)

    fun listPostsByTopicId(topicId: String, paginationRequest: PaginationRequest): PaginationResponse<Post> {
        val limit = paginationRequest.limit
        val page = paginationRequest.page
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val posts = if (page != null) {
            repository.findAllByTopicIdAndIdLessThan(topicId, page, pageable)
        } else {
            repository.findAllByTopicId(topicId, pageable)
        }
        val nextPageToken = posts.lastOrNull()?.id
        return PaginationResponse(posts, nextPageToken)
    }

    fun listPostsByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationResponse<Post> {
        TODO("return the Post Pagination Response containing the keyword")
//        val limit = paginationRequest.limit
//        val page = paginationRequest.page
//        val indexes = listOf(SearchIndex.POSTS, SearchIndex.COMMENTS)
//
//        val pageTimestamp = page?.let { repository.findByIdOrNull(it)?.createdAt?.epochSecond }
//        val postFilter = PostFilter(pageTimestamp)
//        val commentFilter = CommentFilter(pageTimestamp)
//        val queryOptions = QueryOptions(
//            queryString = keyword,
//            filters = listOf(postFilter, commentFilter)
//        )
//        val documents = searchIndexService.searchDocuments(indexes, queryOptions)
//        val postIds = aggregatePostAndCommentSearchResults(documents)
//
//        val posts = mutableListOf<Post>()
//        postIds.forEach { id ->
//            posts.add(find(id)!!)
//        }
//        val limitedPosts = posts.take(limit)
//        val nextPageToken = limitedPosts.lastOrNull()?.id
//        return PaginationResponse(limitedPosts, nextPageToken)
    }

    // CAN BE DELETED
//    private fun aggregatePostAndCommentSearchResults(documents: Map<SearchIndex, List<Document>>): List<String>{
//        val ids = mutableSetOf<String>()
//        documents.forEach { (index, documentList) ->
//            documentList.forEach { document ->
//                val id = if (index.indexId == "comments") {
//                    document.toJsonNode().get("parentId").asText()
//                } else {
//                    document.toJsonNode().get("id").asText()
//                }
//                println(index.indexId)
//                println(id)
//                ids.add(id)
//            }
//        }
//        return ids.toList()
//    }

    fun listPostsByAuthorId(authorId: String): List<Post> = repository.findAllByOwnerId(authorId)

    @Transactional
    fun create(topic: Topic, owner: UserDetails, dto: PostDto): Post {
        val post = Post(
            title = dto.title,
            content = dto.content,
            topic = topic,
            owner = owner,
            attachments = (dto.images!! + dto.videos!!).map {
                attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository)
            },
            comments = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        return repository.save(post)
    }

    @Transactional
    fun update(post: Post, postUpdate: PostUpdate) : Post {
        postUpdate.title?.let { post.title = it }
        postUpdate.content?.let { post.content = it }
        post.updatedAt = Instant.now()

        val attachmentMaintainedAfterDeletion: List<Attachment> = post.attachments.filter { attachment -> attachment.id !in postUpdate.attachmentDelete!! }
        val attachmentAdded = (postUpdate.images!! + postUpdate.videos!!).map {
            attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository) }
        post.attachments = attachmentMaintainedAfterDeletion + attachmentAdded
        return repository.save(post)
    }

    @Transactional
    fun delete(post: Post) {
        post.attachments.forEach { attachmentRepository.delete(it) }
        repository.deleteById(post.id!!)
    }

    @Transactional
    fun addLike(parentId: String, user: UserDetails) {
        val userId = user.id!!
        val like = Like(
            userId = userId,
            parentId = parentId,
        )
        likeRepository.save(like)
        repository.addLike(parentId)
    }

    @Transactional
    fun removeLike(parentId: String, user: UserDetails) {
        val userId = user.id!!
        val like = likeRepository.findByUserIdAndParentId(userId, parentId)
        likeRepository.deleteById(like!!.id!!)
        repository.removeLike(parentId)
    }

    @Transactional
    fun addView(parentId: String, user: UserDetails) {
        val userId = user.id!!
        val view = View(
            userId = userId,
            parentId = parentId,
        )
        viewRepository.save(view)
        repository.addView(parentId)
    }

    fun findLikeByUserIdAndParentId(userId: String, parentId: String): Like? =
        likeRepository.findByUserIdAndParentId(userId, parentId)

    fun findViewByUserIdAndParentId(userId: String, parentId: String): View? =
        viewRepository.findByUserIdAndParentId(userId, parentId)

    fun isPostOwner(post: Post, user: UserDetails): Boolean = post.owner == user
}
