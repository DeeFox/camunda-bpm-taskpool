package io.holunda.camunda.taskpool.example.tasklist.rest.impl

import io.holunda.camunda.taskpool.example.tasklist.auth.CurrentUserService
import io.holunda.camunda.taskpool.example.tasklist.rest.Rest
import io.holunda.camunda.taskpool.example.tasklist.rest.api.ProcessesApi
import io.holunda.camunda.taskpool.example.tasklist.rest.mapper.ProcessDefinitionMapper
import io.holunda.camunda.taskpool.example.tasklist.rest.model.ProcessDefinitionDto
import io.holunda.camunda.taskpool.view.ProcessDefinition
import io.holunda.camunda.taskpool.view.auth.UserService
import io.holunda.camunda.taskpool.view.query.process.ProcessDefinitionsStartableByUserQuery
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
@RequestMapping(Rest.REQUEST_PATH)
class StartableProcessDefinitionController(
  private val currentUserService: CurrentUserService,
  private val userService: UserService,
  private val queryGateway: QueryGateway,
  private val mapper: ProcessDefinitionMapper
) : ProcessesApi {

  override fun getStartableProcesses(@RequestHeader(value = "X-Current-User-ID", required = false) xCurrentUserID: Optional<String>): ResponseEntity<List<ProcessDefinitionDto>> {

    val userIdentifier = xCurrentUserID.orElseGet { currentUserService.getCurrentUser() }
    val user = userService.getUser(userIdentifier)

    val result: List<ProcessDefinition> = queryGateway
      .query(
        ProcessDefinitionsStartableByUserQuery(user = user),
        ResponseTypes.multipleInstancesOf(ProcessDefinition::class.java)
      ).join()

    return ok()
      .body(result.map { mapper.dto(it) })

  }
}
