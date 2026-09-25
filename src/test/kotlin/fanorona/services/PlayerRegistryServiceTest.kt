package fanorona.services

import fanorona.repositories.InMemoryPlayerRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerRegistryServiceTest {
    @Test
    fun `register stores normalized name in the player registry`() {
        val repository = InMemoryPlayerRepository()
        val registry = PlayerRegistryService(repository)

        val player = registry.register("  Анна   Иванова  ")

        assertEquals("Анна Иванова", player.name)
        assertEquals(player, repository.findByName("анна иванова"))
        assertEquals(listOf(player), registry.players())
    }

    @Test
    fun `register rejects blank and duplicate names`() {
        val registry = PlayerRegistryService(InMemoryPlayerRepository())
        registry.register("Анна Иванова")

        assertFailsWith<IllegalArgumentException> { registry.register("   ") }
        assertFailsWith<IllegalArgumentException> { registry.register("  АННА   ИВАНОВА ") }
        assertEquals(1, registry.players().size)
    }
}
