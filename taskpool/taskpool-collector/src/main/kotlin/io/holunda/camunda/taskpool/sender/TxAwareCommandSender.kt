package io.holunda.camunda.taskpool.sender

import io.holunda.camunda.taskpool.api.task.CamundaTaskEvent.Companion.COMPLETE
import io.holunda.camunda.taskpool.api.task.CamundaTaskEvent.Companion.CREATE
import io.holunda.camunda.taskpool.api.task.EnrichedEngineTaskCommand
import io.holunda.camunda.taskpool.enricher.VariablesEnricher
import org.springframework.transaction.support.TransactionSynchronizationAdapter
import org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization

/**
 * Leverages the simple task command sender, by executing it after the TX commit.
 */
class TxAwareCommandSender(
  private val commandGatewayProxy: CommandGatewayProxy,
  private val enricher: VariablesEnricher
) : CommandSender {

  override fun send(command: Any) {
    // enrich before TX commit
    val payload = enrich(command)

    registerSynchronization(object : TransactionSynchronizationAdapter() {
      override fun afterCommit() {
        // try to enrich after TX commit (if the execution still exists)
        commandGatewayProxy.send(enrich(payload))
      }
    })
  }


  /**
   * Enriches the command, if possible.
   * Currently, only CREATE and COMPLETE commands are enriched.
   */
  fun enrich(command: Any) = when (command) {
      is EnrichedEngineTaskCommand -> when (command.eventName) {
        CREATE, COMPLETE -> enricher.enrich(command)
        else -> command
      }
    else -> command
  }
}
