package com.example.shoppingcart.impl

import akka.actor.setup.ActorSystemSetup
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.ActorSystem
import akka.actor.BootstrapSetup
import akka.actor.typed
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import com.example.shoppingcart.impl.ShoppingCart.AddItem
import com.example.shoppingcart.impl.ShoppingCart.Confirmation
import com.example.shoppingcart.impl.ShoppingCart.ItemAdded
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.LagomApplication
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.lightbend.lagom.scaladsl.testkit.TestTopicComponents
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

/**
 * ConfigFactory.load will read the serialization settings from application.conf
 */
class ShoppingCartEntityTypedTestkitSpec
    extends AbstractShoppingCartEntityTypedTestkitSpec(
      EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load)
    )

/**
 * CustomConfigShoppingCartEntityTypedTestkitSpec demonstrates an alternative to ShoppingCartEntityTypedTestkitSpec that
 * uses custom configuration instead of relying on `ConfigFactory.load`
 */
object CustomConfigShoppingCartEntityTypedTestkitSpec {
  val testConfig =
    ConfigFactory.parseString("""
                                |akka.actor {
                                |  serialization-bindings {
                                |    "com.example.shoppingcart.impl.ShoppingCart$CommandSerializable" = jackson-json
                                |  }
                                |}
                                |""".stripMargin)
}

class CustomConfigShoppingCartEntityTypedTestkitSpec
    extends AbstractShoppingCartEntityTypedTestkitSpec(
      EventSourcedBehaviorTestKit.config.withFallback(CustomConfigShoppingCartEntityTypedTestkitSpec.testConfig)
    )

object AbstractShoppingCartEntityTypedTestkitSpec {
  private val userSerializationRegistry = ShoppingCartSerializerRegistry
  // This method is unexpected complexity in order to build a typed ActorSystem with
  // the user's `ShoppingCartSerializerRegistry` registered so that user messages can
  // still use Lagom's play-json serializers with Akka Persistence Typed.
  def typedActorSystem(name: String, config: Config): typed.ActorSystem[Nothing] = {
    val setup: ActorSystemSetup =
      ActorSystemSetup(
        BootstrapSetup(
          classLoader = Some(classOf[AbstractShoppingCartEntityTypedTestkitSpec].getClassLoader),
          config = Some(config),
          None
        ),
        JsonSerializerRegistry.serializationSetupFor(userSerializationRegistry)
      )
    import akka.actor.typed.scaladsl.adapter._
    ActorSystem(name, setup).toTyped
  }

}

abstract class AbstractShoppingCartEntityTypedTestkitSpec(config: Config)
    extends ScalaTestWithActorTestKit(
      AbstractShoppingCartEntityTypedTestkitSpec.typedActorSystem("ShoppingCartEntityTypedTestkitSpec", config)
    )
    with AnyWordSpecLike {

  private def randomId(): String = UUID.randomUUID().toString

  "ShoppingCart" must {
    "add an item" in {
      val entity = EventSourcedBehaviorTestKit[ShoppingCart.Command, ShoppingCart.Event, ShoppingCart](
        system,
        ShoppingCart(PersistenceId("ShoppingCart", randomId()))
      )

      val result = entity.runCommand(AddItem("1", 1, _))
      result.reply shouldBe a[Confirmation]
      result.event shouldBe a[ItemAdded]
    }
  }
}

class ShoppingCartTest extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCluster().withJdbc())(
    new LagomApplication(_) with ShoppingCartComponents with TestTopicComponents {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }
  )
  val system = server.actorSystem.toTyped

  "ShoppingCart" must {
    "add an item" in {
      val entity = EventSourcedBehaviorTestKit[ShoppingCart.Command, ShoppingCart.Event, ShoppingCart](
        system,
        ShoppingCart(PersistenceId("ShoppingCart", "test-entity-for-jdbc"))
      )

      val result = entity.runCommand(AddItem("1", 1, _))
      result.reply shouldBe a[Confirmation]
      result.event shouldBe a[ItemAdded]
    }
  }
}
