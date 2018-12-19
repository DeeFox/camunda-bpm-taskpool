package io.holunda.camunda.taskpool.core.task

import io.holunda.camunda.taskpool.api.business.addCorrelation
import io.holunda.camunda.taskpool.api.business.newCorrelations
import io.holunda.camunda.taskpool.api.task.*
import org.axonframework.test.aggregate.AggregateTestFixture
import org.camunda.bpm.engine.variable.Variables
import org.camunda.bpm.engine.variable.Variables.stringValue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.*

class TaskAggregateEngineCommandTest {

  private val fixture: AggregateTestFixture<TaskAggregate> = AggregateTestFixture<TaskAggregate>(TaskAggregate::class.java)
  private lateinit var now: Date
  private lateinit var now2: Date

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
    now2 = Date.from(Instant.now().plusSeconds(1000))
  }

  @Test
  fun `should create task`() {
    fixture
      .givenNoPriorActivity()
      .`when`(
        CreateTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          followUpDate = now2,
          dueDate = now,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .expectEvents(
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
          followUpDate = now2,
          dueDate = now,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
  }

  @Test
  fun `should complete task`() {
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
          followUpDate = now2,
          dueDate = now,
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        CompleteTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          businessKey = "business123",
          enriched = true,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          followUpDate = now2,
          dueDate = now,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        ))
      .expectEvents(
        TaskCompletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      )
  }

  @Test
  fun `should assign task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          taskDefinitionKey = "foo",
          formKey = "some",
          sourceReference = processReference
        ))
      .`when`(
        AssignTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        ))
      .expectEvents(
        TaskAssignedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          formKey = "some",
          owner = "kermit",
          taskDefinitionKey = "foo",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      )
  }


  @Test
  fun `should not re-assign task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          taskDefinitionKey = "foo",
          formKey = "some",
          sourceReference = processReference
        ),
        TaskAssignedEngineEvent(
          id = "4711",
          name = "Foo",
          taskDefinitionKey = "foo",
          assignee = "kermit",
          sourceReference = processReference
        )
      )
      .`when`(
        AssignTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          taskDefinitionKey = "foo",
          assignee = "kermit",
          sourceReference = processReference
        ))
      .expectNoEvents()
  }


  @Test
  fun `should not complete deleted task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
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
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        ))
      .`when`(
        CompleteTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          businessKey = "business123",
          enriched = true,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      ).expectNoEvents()
  }

  @Test
  fun `should not complete completed task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
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
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789")
        ))
      .`when`(
        CompleteTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          businessKey = "business123",
          enriched = true,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      ).expectNoEvents()
  }

  @Test
  fun `should not assign completed task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
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
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789")
        ))
      .`when`(
        AssignTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "another",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      ).expectNoEvents()
  }

  @Test
  fun `should not assign deleted task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
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
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        ))
      .`when`(
        AssignTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "another",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      ).expectNoEvents()
  }
  @Test
  fun `should not delete completed task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
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
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789")
        ))
      .`when`(
        DeleteTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,
          deleteReason = "Not possible"
        )
      ).expectNoEvents()
  }


  @Test
  fun `should not delete deleted task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
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
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        ))
      .`when`(
        DeleteTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,
          deleteReason = "Not possible"
        )
      ).expectNoEvents()
  }

  @Test
  fun `should delete task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        DeleteTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          businessKey = "business123",
          enriched = true,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        ))
      .expectEvents(
        TaskDeletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        )
      )
  }

  @Test
  fun `should update task attributes`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        UpdateAttributeTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,
          name = "New name",
          description = "New description",
          dueDate = now,
          followUpDate = now2,
          assignee = "piggy",
          owner = "gonzo",
          priority = 13
        )
      )
      .expectEvents(
        TaskAttributeUpdatedEngineEvent(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,

          name = "New name",
          description = "New description",
          dueDate = now,
          followUpDate = now2,
          assignee = "piggy",
          owner = "gonzo",
          priority = 13
        )
      )
  }

  @Test
  fun `should not update completed task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
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
          enriched = true,
          sourceReference = processReference,
          followUpDate = now2,
          dueDate = now,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789")
        ))
      .`when`(
        UpdateAttributeTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,

          name = "New name",
          description = "New description",
          dueDate = now,
          followUpDate = now2,
          assignee = "piggy",
          owner = "gonzo",
          priority = 13
        )
      ).expectNoEvents()
  }

  @Test
  fun `should not update deleted task`() {
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
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ),
        TaskDeletedEngineEvent(
          id = "4711",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          deleteReason = "deleted"
        ))
      .`when`(
        UpdateAttributeTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          sourceReference = processReference,

          name = "New name",
          description = "New description",
          dueDate = now,
          followUpDate = now2,
          assignee = "piggy",
          owner = "gonzo",
          priority = 13
        )
      ).expectNoEvents()
  }


}
