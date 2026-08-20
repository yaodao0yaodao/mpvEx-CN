package app.marlboroadvance.mpvex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class RendererBackendPolicyTest {
  @Test
  fun `HDR prefers gpu-next Vulkan when supported`() {
    val backend = selectRendererBackend(false, false, true, false, true)

    assertEquals(RendererBackend("gpu-next", "vulkan", "androidvk"), backend)
  }

  @Test
  fun `HDR falls back to gpu-next OpenGL without Vulkan`() {
    val backend = selectRendererBackend(false, false, false, false, true)

    assertEquals(RendererBackend("gpu-next", "opengl", "android"), backend)
  }

  @Test
  fun `Anime4K avoids unsupported gpu-next OpenGL combination`() {
    val backend = selectRendererBackend(true, false, false, true, false)

    assertEquals(RendererBackend("gpu", "opengl", "android"), backend)
  }

  @Test
  fun `user Vulkan preference remains effective without HDR`() {
    val backend = selectRendererBackend(false, true, true, false, false)

    assertEquals(RendererBackend("gpu", "vulkan", "androidvk"), backend)
  }
}
