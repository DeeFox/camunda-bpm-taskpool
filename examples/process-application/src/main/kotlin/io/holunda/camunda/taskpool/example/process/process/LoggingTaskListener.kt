package io.holunda.camunda.taskpool.example.process.process

import io.holunda.camunda.taskpool.collector.TaskEventCollectorService
import mu.KLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class LoggingTaskListener {

  companion object : KLogging()

  @EventListener(condition = "#task.eventName.equals('create')")
  @Order(TaskEventCollectorService.ORDER - 9)
  fun logTaskCreation(task: DelegateTask) {
    logger.debug { "Created task ${task.id} of type ${task.taskDefinitionKey}" }
  }

}
