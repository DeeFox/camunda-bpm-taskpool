package io.holunda.camunda.taskpool.example.process.process

import io.holunda.camunda.taskpool.collector.TaskEventCollectorService.Companion.ORDER
import io.holunda.camunda.taskpool.example.process.process.ProcessApproveRequest.Variables.APPLICANT
import io.holunda.camunda.taskpool.example.process.process.ProcessApproveRequest.Variables.ORIGINATOR
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Example description setting listener to demonstrate that task pool is aware of listener order.
 */
@Component
class DescriptionSettingTaskListener {

  @EventListener(condition = "#task.eventName.equals('create')")
  @Order(ORDER - 10)
  fun changeDescription(task: DelegateTask) {
    task.description = when (task.taskDefinitionKey) {
      ProcessApproveRequest.Elements.APPROVE_REQUEST -> "Please approve request ${task.execution.businessKey} from ${task.variables[ORIGINATOR]} on behalf of ${(task.variables[APPLICANT])}."
      ProcessApproveRequest.Elements.AMEND_REQUEST -> "Please amend the approval request ${task.execution.businessKey}."
      else -> ""
    }
  }
}
