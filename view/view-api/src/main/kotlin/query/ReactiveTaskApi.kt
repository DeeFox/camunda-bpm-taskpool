package io.holunda.camunda.taskpool.view.query

import io.holunda.camunda.taskpool.view.Task
import io.holunda.camunda.taskpool.view.TaskWithDataEntries
import io.holunda.camunda.taskpool.view.query.task.*
import java.util.concurrent.CompletableFuture

/**
 * Reactive API to retrieve tasks.
 */
interface ReactiveTaskApi {

  /**
   * Queries user tasks with data.
   */
  fun query(query: TasksWithDataEntriesForUserQuery): CompletableFuture<TasksWithDataEntriesQueryResult>

  /**
   * Queries user tasks for task id.
   */
  fun query(query: TaskWithDataEntriesForIdQuery): CompletableFuture<TaskWithDataEntries?>

  /**
   * Count user tasks for applications.
   */
  fun query(query: TaskCountByApplicationQuery): CompletableFuture<List<ApplicationWithTaskCount>>

  /**
   * Tasks for user.
   */
  fun query(query: TasksForUserQuery): CompletableFuture<TaskQueryResult>

  /**
   * Task for id.
   */
  fun query(query: TaskForIdQuery): CompletableFuture<Task?>

}
