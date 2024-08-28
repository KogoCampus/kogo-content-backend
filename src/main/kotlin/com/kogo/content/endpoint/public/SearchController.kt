package com.kogo.content.endpoint.public

import com.kogo.content.endpoint.common.ApiResponse
import com.kogo.content.service.GroupService
import com.kogo.content.service.meilisearch.MeilisearchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("media")
class SearchController @Autowired constructor(
    private val meiliSearchService: MeilisearchService
) {
    @GetMapping("groups")
    fun searchGroups(
        @RequestParam(name = "q", defaultValue = "") query: String
    ): ApiResponse {
        return ApiResponse.success(meiliSearchService.searchGroups(query))
    }

    //TO BE DELETED
    @DeleteMapping("groups/search/{id}")
    fun deleteGroup(@PathVariable("id") groupId: String) = ApiResponse.success(meiliSearchService.deleteGroup(groupId))

}
