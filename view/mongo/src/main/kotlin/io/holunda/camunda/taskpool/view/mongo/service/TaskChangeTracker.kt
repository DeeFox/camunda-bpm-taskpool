package io.holunda.camunda.taskpool.view.mongo.service

import com.mongodb.client.model.changestream.OperationType
import io.holunda.camunda.taskpool.api.business.dataIdentityString
import io.holunda.camunda.taskpool.view.Task
import io.holunda.camunda.taskpool.view.TaskWithDataEntries
import io.holunda.camunda.taskpool.view.mongo.repository.*
import io.holunda.camunda.taskpool.view.query.task.ApplicationWithTaskCount
import mu.KLogging
import org.bson.BsonValue
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.mongodb.core.ChangeStreamEvent
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.function.Function.identity
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Observes the change stream on the mongo db and provides `Flux`es of changes for the various result types of queries. Also makes sure that tasks marked as
 * deleted are 'really' deleted shortly after.
 * Only active if `camunda.taskpool.view.mongo.changeTrackingMode` is set to `CHANGE_STREAM`.
 */
@Component
@ConditionalOnProperty(prefix = "camunda.taskpool.view.mongo", name = ["changeTrackingMode"], havingValue = "CHANGE_STREAM", matchIfMissing = false)
class TaskChangeTracker(
  private val taskRepository: TaskRepository,
  private val dataEntryRepository: DataEntryRepository
) {
  companion object : KLogging()

  private lateinit var changeStream: Flux<ChangeStreamEvent<TaskDocument>>
  private lateinit var trulyDeleteChangeStreamSubscription: Disposable

  /**
   * Create subscription.
   */
  @PostConstruct
  fun createSubscription() {
    var lastSeenResumeToken: BsonValue? = null
    changeStream = Mono.fromSupplier { taskRepository.getTaskUpdates(lastSeenResumeToken) }
      .flatMapMany(identity())
      .doOnNext { event ->
        val resumeToken = event.resumeToken
        if (resumeToken != null) lastSeenResumeToken = resumeToken
      }
      .filter { event ->
        when (event.operationType) {
          OperationType.INSERT, OperationType.UPDATE, OperationType.REPLACE -> {
            logger.debug { "Got ${event.operationType?.value} event: $event" }
            true
          }
          else -> {
            logger.trace { "Ignoring ${event.operationType?.value} event: $event" }
            false
          }
        }
      }
      .retryBackoff(Long.MAX_VALUE, Duration.ofMillis(100), Duration.ofSeconds(10))
      .share()

    // Truly delete documents that have been marked deleted
    trulyDeleteChangeStreamSubscription = changeStream
      .filter { event -> event.body?.deleted == true }
      .concatMap { event ->
        taskRepository.deleteById(event.body.id)
          .doOnSuccess { logger.trace { "Deleted task ${event.body.id} from database." } }
      }
      .subscribe()
  }

  /**
   * Clear subscription.
   */
  @PreDestroy
  fun clearSubscription() {
    trulyDeleteChangeStreamSubscription.dispose()
  }

  /**
   * Adopt changes to task count by application stream.
   */
  fun trackTaskCountsByApplication(): Flux<ApplicationWithTaskCount> = changeStream
    .window(Duration.ofSeconds(1))
    .concatMap { it.reduce(setOf<String>()) { applicationNames, event -> applicationNames + event.body.sourceReference.applicationName } }
    .concatMap { Flux.fromIterable(it) }
    .concatMap { taskRepository.findTaskCountForApplication(it) }

  /**
   * Adopt changes to task update stream.
   */
  fun trackTaskUpdates(): Flux<Task> = changeStream
    .map { event -> event.body.task() }

  /**
   * Adopt changes to task with data entries update stream.
   */
  fun trackTaskWithDataEntriesUpdates(): Flux<TaskWithDataEntries> = changeStream
    .concatMap { event ->
      val task = event.body.task()
      this.dataEntryRepository.findAllById(task.correlations.map { dataIdentityString(entryType = it.key, entryId = it.value.toString()) })
        .map { it.dataEntry() }
        .collectList()
        .map { TaskWithDataEntries(task = task, dataEntries = it) }
    }
}
