package io.holunda.camunda.taskpool.core.task

import io.holunda.camunda.taskpool.api.business.addCorrelation
import io.holunda.camunda.taskpool.api.business.newCorrelations
import io.holunda.camunda.taskpool.api.task.*
import org.axonframework.test.aggregate.AggregateTestFixture
import org.camunda.bpm.engine.variable.Variables
import org.junit.Before
import org.junit.Test
import java.util.*

class TaskAggregateClaimTest {


  private val fixture: AggregateTestFixture<TaskAggregate> = AggregateTestFixture<TaskAggregate>(TaskAggregate::class.java)
  private lateinit var now: Date

  private val processReference = ProcessReference(
    definitionKey = "process_key",
    instanceId = "0815",
    executionId = "12345",
    definitionId = "76543",
    name = "My process",
    applicationName = "myExample"
  )

  @Before
  fun setUp() {
    now = Date()
  }


  @Test
  fun `should claim unassigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = null,
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        ClaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy"
        )
      ).expectEvents(
        TaskClaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy",
          formKey = "some"
        )
      )
  }

  @Test
  fun `should claim assigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        ClaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy"
        )
      ).expectEvents(
        TaskUnclaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          formKey = "some"
        ),
        TaskClaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy",
          formKey = "some"
        )
      )
  }

  @Test
  fun `should unclaim assigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        UnclaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        )
      ).expectEvents(
        TaskUnclaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          formKey = "some"
        )
      )
  }

  @Test
  fun `should unclaim unassigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = null,
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        UnclaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        )
      ).expectNoEvents()
  }


  @Test
  fun `should not unclaim task if already deleted`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = null,
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ),
        TaskDeletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          deleteReason = "Test delete"
        ))
      .`when`(
        UnclaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        )
      ).expectNoEvents()
  }

  @Test
  fun `should not unclaim task if already completed`() {

    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          assignee = "kermit",
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ),
        TaskCompletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference
        ))
      .`when`(
        UnclaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        )
      )
      .expectNoEvents()
  }

  @Test
  fun `should not claim task if already deleted`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = null,
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ),
        TaskDeletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          deleteReason = "Test delete"
        ))
      .`when`(
        ClaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "fozzy"
        )
      ).expectNoEvents()
  }

  @Test
  fun `should not claim task if already completed`() {

    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          assignee = "kermit",
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", Variables.stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ),
        TaskCompletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference
        ))
      .`when`(
        ClaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "fozzy"
        )
      )
      .expectNoEvents()
  }


}
