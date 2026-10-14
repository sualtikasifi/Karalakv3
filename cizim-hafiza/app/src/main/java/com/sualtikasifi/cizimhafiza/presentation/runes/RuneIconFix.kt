package com.sualtikasifi.cizimhafiza.presentation.runes

/**
 * How far each rune's picture has to move (in tile sizes, on top of the plate's own medallion shift) to sit exactly in the
 * middle of its medallion. Measured from rendered tiles (frame alone against frame with icon), one entry per rune and
 * state: 0 = owned, 1 = fully upgraded (ringed plate), 2 = open but not bought. Regenerate after changing icons or plates.
 */
internal object RuneIconFix {
    private val table: Map<String, Pair<Float, Float>> = mapOf(
        "root:0" to (0.0009f to -0.0164f),
        "root:1" to (0.0009f to -0.0164f),
        "root:2" to (0.0009f to -0.0164f),
        "word_gold:0" to (-0.0043f to 0.0022f),
        "word_gold:1" to (-0.0198f to -0.0225f),
        "word_gold:2" to (-0.0072f to -0.0099f),
        "login_gold:0" to (-0.0035f to 0.0033f),
        "login_gold:1" to (-0.0189f to -0.0213f),
        "login_gold:2" to (-0.0064f to -0.0090f),
        "daily_1:0" to (0.0040f to 0.0038f),
        "daily_1:1" to (-0.0120f to -0.0226f),
        "daily_1:2" to (0.0004f to -0.0062f),
        "win_gold:0" to (-0.0034f to 0.0083f),
        "win_gold:1" to (-0.0188f to -0.0164f),
        "win_gold:2" to (-0.0064f to -0.0039f),
        "chest_gold:0" to (-0.0098f to 0.0078f),
        "chest_gold:1" to (-0.0224f to -0.0197f),
        "chest_gold:2" to (-0.0098f to -0.0075f),
        "chest_joker:0" to (-0.0081f to 0.0024f),
        "chest_joker:1" to (-0.0205f to -0.0244f),
        "chest_joker:2" to (-0.0082f to -0.0128f),
        "disc_1:0" to (-0.0069f to -0.0094f),
        "disc_1:1" to (-0.0212f to -0.0273f),
        "disc_1:2" to (-0.0098f to -0.0155f),
        "dj_1:0" to (-0.0013f to -0.0014f),
        "dj_1:1" to (-0.0204f to -0.0305f),
        "dj_1:2" to (-0.0067f to -0.0137f),
        "streak_key:0" to (-0.0050f to -0.0352f),
        "streak_key:1" to (-0.0050f to -0.0352f),
        "streak_key:2" to (-0.0078f to -0.0400f),
        "chest_key2:0" to (-0.0064f to -0.0242f),
        "chest_key2:1" to (-0.0064f to -0.0242f),
        "chest_key2:2" to (-0.0089f to -0.0346f),
        "luck_key:0" to (-0.0117f to -0.0400f),
        "luck_key:1" to (-0.0117f to -0.0400f),
        "luck_key:2" to (-0.0084f to -0.0480f),
        "time_1:0" to (0.0068f to -0.0099f),
        "time_1:1" to (-0.0139f to -0.0211f),
        "time_1:2" to (0.0012f to -0.0065f),
        "ad_1:0" to (0.0011f to -0.0107f),
        "ad_1:1" to (-0.0165f to -0.0189f),
        "ad_1:2" to (-0.0048f to -0.0075f),
        "slot_key:0" to (-0.0022f to -0.0289f),
        "slot_key:1" to (-0.0022f to -0.0289f),
        "slot_key:2" to (-0.0079f to -0.0522f),
        "fr_1:0" to (-0.0014f to 0.0066f),
        "fr_1:1" to (-0.0199f to -0.0203f),
        "fr_1:2" to (-0.0081f to -0.0085f),
        "fr_2:0" to (-0.0005f to -0.0105f),
        "fr_2:1" to (-0.0191f to -0.0383f),
        "fr_2:2" to (-0.0067f to -0.0260f),
        "fr_key:0" to (0.0011f to -0.0184f),
        "fr_key:1" to (0.0011f to -0.0184f),
        "fr_key:2" to (-0.0038f to -0.0455f),
        "st_1:0" to (-0.0045f to 0.0007f),
        "st_1:1" to (-0.0292f to -0.0274f),
        "st_1:2" to (-0.0099f to -0.0179f),
        "st_3:0" to (-0.0103f to -0.0041f),
        "st_3:1" to (-0.0308f to -0.0299f),
        "st_3:2" to (-0.0175f to -0.0190f),
        "st_5:0" to (0.0077f to -0.0112f),
        "st_5:1" to (-0.0090f to -0.0408f),
        "st_5:2" to (0.0031f to -0.0293f),
        "st_6:0" to (-0.0008f to 0.0064f),
        "st_6:1" to (-0.0186f to -0.0204f),
        "st_6:2" to (-0.0064f to -0.0083f),
        "st_7:0" to (0.0026f to 0.0049f),
        "st_7:1" to (-0.0161f to -0.0224f),
        "st_7:2" to (-0.0036f to -0.0103f),
        "st_key:0" to (-0.0028f to -0.0001f),
        "st_key:1" to (-0.0028f to -0.0001f),
        "st_key:2" to (-0.0069f to -0.0475f),
        "xp_1:0" to (-0.0023f to -0.0126f),
        "xp_1:1" to (-0.0230f to -0.0343f),
        "xp_1:2" to (-0.0081f to -0.0221f),
        "ins_key:0" to (-0.0002f to -0.0075f),
        "ins_key:1" to (-0.0002f to -0.0075f),
        "ins_key:2" to (-0.0053f to -0.0413f)
    )

    /** The padlock on a closed plate: measured the same way (the keystone plates sit higher than the ordinary ones). */
    fun lockShift(rune: com.sualtikasifi.cizimhafiza.domain.model.Rune): Pair<Float, Float> = when {
        rune.id == com.sualtikasifi.cizimhafiza.domain.model.RuneTree.ROOT_ID -> -0.0023f to -0.0091f
        rune.isKeystone -> -0.0031f to -0.0335f
        else -> -0.0031f to -0.0123f
    }

    fun shift(id: String, state: Int): Pair<Float, Float> = table["$id:$state"]?.let { it.first to it.second } ?: (0f to 0f)
}
