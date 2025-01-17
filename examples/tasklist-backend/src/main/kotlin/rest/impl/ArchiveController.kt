package io.holunda.camunda.taskpool.example.tasklist.rest.impl

import io.holunda.camunda.taskpool.example.tasklist.auth.CurrentUserService
import io.holunda.camunda.taskpool.example.tasklist.rest.Rest
import io.holunda.camunda.taskpool.example.tasklist.rest.api.BolistApi
import io.holunda.camunda.taskpool.example.tasklist.rest.mapper.TaskWithDataEntriesMapper
import io.holunda.camunda.taskpool.example.tasklist.rest.model.DataEntryDto
import io.holunda.camunda.taskpool.view.auth.UserService
import io.holunda.camunda.taskpool.view.query.data.DataEntriesForUserQuery
import io.holunda.camunda.taskpool.view.query.data.DataEntriesQueryResult
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(Rest.REQUEST_PATH)
class ArchiveController(
  private val queryGateway: QueryGateway,
  private val currentUserService: CurrentUserService,
  private val userService: UserService,
  private val mapper: TaskWithDataEntriesMapper
) : BolistApi {

  override fun getBos(
    @RequestParam(value = "filter") filters: List<String>,
    @RequestParam(value = "page") page: Optional<Int>,
    @RequestParam(value = "size") size: Optional<Int>,
    @RequestParam(value = "sort") sort: Optional<String>,
    @RequestHeader(value = "X-Current-User-ID", required = false) xCurrentUserID: Optional<String>
  ): ResponseEntity<List<DataEntryDto>> {

    val userIdentifier = xCurrentUserID.orElseGet { currentUserService.getCurrentUser() }
    val user = userService.getUser(userIdentifier)


    @Suppress("UNCHECKED_CAST")
    val result: DataEntriesQueryResult = queryGateway.query(DataEntriesForUserQuery(
      user = user,
      page = page.orElse(1),
      size = size.orElse(Int.MAX_VALUE),
      sort = sort.orElseGet { "" },
      filters = filters
    ), ResponseTypes.instanceOf(DataEntriesQueryResult::class.java)).join()

    val responseHeaders = HttpHeaders().apply {
      this[TasksController.HEADER_ELEMENT_COUNT] = result.totalElementCount.toString()
    }

    return ResponseEntity.ok()
      .headers(responseHeaders)

      .body(result.elements.map { mapper.dto(it) })
  }
}
