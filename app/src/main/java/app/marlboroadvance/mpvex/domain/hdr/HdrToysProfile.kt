package app.marlboroadvance.mpvex.domain.hdr

enum class HdrToysProfile(
  val targetPrim: String,
  val targetTrc: String,
  val shaderPaths: List<String>,
  val shaderOptions: List<Pair<String, String>> = emptyList(),
) {
  BT_2100_PQ(
    targetPrim = "bt.2020",
    targetTrc = "pq",
    shaderPaths =
      listOf(
        "utils/clip_both.glsl",
        "transfer-function/pq_inv.glsl",
        "tone-mapping/astra.glsl",
        "gamut-mapping/bottosson.glsl",
        "transfer-function/bt1886.glsl",
      ),
    shaderOptions = listOf("auto_exposure_limit_positive" to "1.02"),
  ),
  BT_2100_HLG(
    targetPrim = "bt.2020",
    targetTrc = "hlg",
    shaderPaths =
      listOf(
        "utils/clip_both.glsl",
        "transfer-function/hlg_inv.glsl",
        "tone-mapping/astra.glsl",
        "gamut-mapping/bottosson.glsl",
        "transfer-function/bt1886.glsl",
      ),
  ),
  BT_2020(
    targetPrim = "bt.2020",
    targetTrc = "bt.1886",
    shaderPaths =
      listOf(
        "transfer-function/bt1886_inv.glsl",
        "gamut-mapping/bottosson.glsl",
        "transfer-function/bt1886.glsl",
      ),
  ),
  ;

  val shaderOptionsValue: String
    get() = shaderOptions.joinToString(",") { (name, value) -> "$name=$value" }
}
