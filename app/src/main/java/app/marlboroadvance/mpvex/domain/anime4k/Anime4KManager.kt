package app.marlboroadvance.mpvex.domain.anime4k

import android.content.Context

import java.io.File
import java.io.FileOutputStream

/**
 * Anime4K Manager
 * Manages GLSL shaders for real-time anime upscaling
 */
class Anime4KManager(private val context: Context) {

  companion object {
    private const val SHADER_DIR = "shaders"
    private const val SHADER_BUNDLE_VERSION = "artcnn-1.6.2-anisd-2026-06"
  }

  // Shader quality levels
  enum class Quality(val suffix: String, val titleRes: Int) {
    FAST("S", app.marlboroadvance.mpvex.R.string.anime4k_quality_fast),
    BALANCED("M", app.marlboroadvance.mpvex.R.string.anime4k_quality_balanced),
    HIGH("L", app.marlboroadvance.mpvex.R.string.anime4k_quality_high)
  }

  // Anime4K modes
  enum class Mode(val titleRes: Int) {
    OFF(app.marlboroadvance.mpvex.R.string.anime4k_mode_off),
    A(app.marlboroadvance.mpvex.R.string.anime4k_mode_a),
    B(app.marlboroadvance.mpvex.R.string.anime4k_mode_b),
    C(app.marlboroadvance.mpvex.R.string.anime4k_mode_c),
    C_PLUS(app.marlboroadvance.mpvex.R.string.anime4k_mode_c_plus),
    ARTCNN_C4F32(app.marlboroadvance.mpvex.R.string.artcnn_mode_c4f32),
    ARTCNN_C4F32_DN(app.marlboroadvance.mpvex.R.string.artcnn_mode_c4f32_dn),
    ARTCNN_C4F32_DS(app.marlboroadvance.mpvex.R.string.artcnn_mode_c4f32_ds),
    ANISD_ARTCNN_C4F32(app.marlboroadvance.mpvex.R.string.artcnn_mode_anisd),
    ANISD_ARTCNN_C4F32_CMP(app.marlboroadvance.mpvex.R.string.artcnn_mode_anisd_cmp),
    ANI4KV2_ARTCNN_C4F32_CMP(app.marlboroadvance.mpvex.R.string.artcnn_mode_ani4kv2_cmp),
    ;

    val usesAnime4KQuality: Boolean
      get() = this == A || this == B || this == C || this == C_PLUS
  }

  private var shaderDir: File? = null
  private var isInitialized = false

  /**
   * Initialize: copy shaders from assets to internal storage
   * This must be called and complete successfully before using getShaderChain()
   */
  fun initialize(): Boolean {
    if (isInitialized) {
      return true
    }
    
    return try {
      // Create shader directory
      shaderDir = File(context.filesDir, SHADER_DIR)
      if (!shaderDir!!.exists()) {
        val created = shaderDir!!.mkdirs()
        if (!created) {
          return false
        }
      }

      val versionFile = File(shaderDir, ".bundle-version")
      val forceRefresh = versionFile.readTextOrNull() != SHADER_BUNDLE_VERSION

      // List and copy all shader files from assets
      val shaderFiles = context.assets.list(SHADER_DIR) ?: emptyArray()

      for (fileName in shaderFiles) {
        if (fileName.endsWith(".glsl")) {
          copyShaderFromAssets(fileName, forceRefresh)
        }
      }
      if (forceRefresh) versionFile.writeText(SHADER_BUNDLE_VERSION)
      
      isInitialized = true
      true
    } catch (e: Exception) {
      isInitialized = false
      false
    }
  }

  private fun copyShaderFromAssets(fileName: String, force: Boolean): Boolean {
    val destFile = File(shaderDir, fileName)
    
    // Skip if file already exists and is valid
    if (!force && destFile.exists() && destFile.length() > 0) {
      return false
    }

    try {
      context.assets.open("$SHADER_DIR/$fileName").use { input ->
        FileOutputStream(destFile).use { output ->
          input.copyTo(output)
        }
      }
      return true
    } catch (e: Exception) {
      return false
    }
  }

  /**
   * Get shader chain for the specified mode and quality
   * Returns empty string if mode is OFF or initialization failed
   */
  fun getShaderChain(mode: Mode, quality: Quality): String {
    if (mode == Mode.OFF) {
      return ""
    }

    if (!isInitialized) {
      return ""
    }

    if (shaderDir == null || !shaderDir!!.exists()) {
      return ""
    }

    val shaders = mutableListOf<String>()
    val q = quality.suffix

    // Always add Clamp_Highlights (prevent ringing)
    shaders.add(getShaderPath("Anime4K_Clamp_Highlights.glsl"))

    // Add shaders based on mode
    when (mode) {
      Mode.A -> {
        // Mode A: Restore -> Upscale -> Upscale
        shaders.add(getShaderPath("Anime4K_Restore_CNN_$q.glsl"))
        shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
        shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
        shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
      }
      Mode.B -> {
        // Mode B: Restore_Soft -> Upscale -> Upscale
        shaders.add(getShaderPath("Anime4K_Restore_CNN_Soft_$q.glsl"))
        shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
        shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
        shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
      }
      Mode.C -> {
        // Mode C: Upscale_Denoise -> Upscale
        shaders.add(getShaderPath("Anime4K_Upscale_Denoise_CNN_x2_$q.glsl"))
        shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
        shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
      }
      Mode.C_PLUS -> {
        // Mode C+A: Upscale_Denoise -> Restore -> Upscale
        shaders.add(getShaderPath("Anime4K_Upscale_Denoise_CNN_x2_$q.glsl"))
        shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
        shaders.add(getShaderPath("Anime4K_Restore_CNN_$q.glsl"))
        shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
      }
      Mode.ARTCNN_C4F32 -> shaders.add(getShaderPath("ArtCNN_C4F32.glsl"))
      Mode.ARTCNN_C4F32_DN -> shaders.add(getShaderPath("ArtCNN_C4F32_DN.glsl"))
      Mode.ARTCNN_C4F32_DS -> shaders.add(getShaderPath("ArtCNN_C4F32_DS.glsl"))
      Mode.ANISD_ARTCNN_C4F32 -> shaders.add(getShaderPath("AniSD_ArtCNN_C4F32_i4.glsl"))
      Mode.ANISD_ARTCNN_C4F32_CMP -> shaders.add(getShaderPath("AniSD_ArtCNN_C4F32_i4_CMP.glsl"))
      Mode.ANI4KV2_ARTCNN_C4F32_CMP -> shaders.add(getShaderPath("Ani4Kv2_ArtCNN_C4F32_i2_CMP.glsl"))
      Mode.OFF -> {
        // Already handled
      }
    }

    // Validate that all shader files exist
    val missingShaders = shaders.filter { path ->
      !File(path).exists()
    }
    
    if (missingShaders.isNotEmpty()) {
      return ""
    }

    // Join with colon separator
    val chain = shaders.joinToString(":")
    return chain
  }

  private fun getShaderPath(fileName: String): String {
    return File(shaderDir, fileName).absolutePath
  }

  private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()
}
