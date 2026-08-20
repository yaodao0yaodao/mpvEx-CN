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

    assertEquals(RendererBackend("gpu-next", "opengl", "android"), backend)
  }

  @Test
  fun `user Vulkan preference remains effective without HDR`() {
    val backend = selectRendererBackend(false, true, true, false, false)

    assertEquals(RendererBackend("gpu", "vulkan", "androidvk"), backend)
  }

  @Test
  fun `hardware plus keeps gpu-next but forces Android OpenGL`() {
    val backend = selectRendererBackend(true, true, true, true, true, hardwarePlusMode = true)

    assertEquals(RendererBackend("gpu-next", "opengl", "android"), backend)
  }

  @Test
  fun `decoder override and stored priority map to mpv hwdec values`() {
    assertEquals("mediacodec-copy", initialHwdecValue(Decoder.HW, Decoder.priorityModes))
    assertEquals("mediacodec", initialHwdecValue(Decoder.HWPlus, Decoder.priorityModes))
    assertEquals("mediacodec,mediacodec-copy,no", initialHwdecValue(null, Decoder.priorityModes))
    assertEquals("no", decoderPriorityHwdecValue(listOf(Decoder.SW, Decoder.HWPlus, Decoder.HW)))
  }
}
