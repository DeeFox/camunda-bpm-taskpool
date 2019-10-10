package io.holunda.camunda.taskpool.core

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.NoTypePermission
import com.thoughtworks.xstream.security.NullPermission
import com.thoughtworks.xstream.security.PrimitiveTypePermission
import io.holunda.camunda.taskpool.core.task.TaskAggregate
import org.axonframework.eventsourcing.EventSourcingRepository
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.serialization.RevisionResolver
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.xml.XStreamSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


@Configuration
@ComponentScan
class TaskPoolCoreConfiguration {

  @Bean
  fun taskAggregateRepository(eventStore: EventStore): EventSourcingRepository<TaskAggregate> {
    return EventSourcingRepository
      .builder(TaskAggregate::class.java)
      .eventStore(eventStore)
      .build()
  }

  @Bean
  @ConditionalOnMissingBean
  fun axonFrameworkSerializer(revisionResolver: RevisionResolver, @Qualifier("xStreamWhitelist") whitelist: Array<String> ) : Serializer {
    val xstream = XStream()
    xstream.addPermission(NoTypePermission.NONE)
    xstream.addPermission(NullPermission.NULL)
    xstream.addPermission(PrimitiveTypePermission.PRIMITIVES)
    xstream.allowTypesByWildcard(whitelist)

    return XStreamSerializer.builder()
      .xStream(xstream)
      .revisionResolver(revisionResolver)
      .build()
  }

  @Bean
  @ConditionalOnMissingBean
  @Qualifier("xStreamWhitelist")
  fun xStreamWhitelist() : Array<String> {
    return arrayOf(
      "org.axonframework.**",
      "java.util.concurrent.*",
      "io.holunda.camunda.taskpool.**",
      "java.lang.String"
    )
  }
}
