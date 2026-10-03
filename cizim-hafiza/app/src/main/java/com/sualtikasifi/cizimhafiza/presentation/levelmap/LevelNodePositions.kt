package com.sualtikasifi.cizimhafiza.presentation.levelmap

/**
 * Where the ten stops of each world sit on that world's artwork (bg_world_N), as percentages of the picture's
 * width and height, level 1 first. The path is painted into the picture, so the stops are placed by hand.
 */
internal val LevelNodePositions: Map<Int, List<Pair<Float, Float>>> = mapOf(
    1 to listOf(37.2f to 93f, 36f to 83f, 61.1f to 79f, 66f to 72f, 68.5f to 60f, 66.3f to 48f, 80.4f to 38f, 53.3f to 28f, 69.6f to 20f, 68.9f to 14f),
    2 to listOf(39.2f to 92f, 62.0f to 82f, 71.3f to 72f, 49.3f to 62f, 31.5f to 54f, 52f to 46f, 78.6f to 38f, 64.7f to 30f, 79.2f to 22f, 72f to 16f),
    3 to listOf(47.9f to 95f, 81.7f to 86f, 66.1f to 77f, 49.3f to 70f, 38.4f to 62f, 72.3f to 52f, 50.8f to 43f, 50.8f to 33f, 60f to 26f, 65f to 18f),
    4 to listOf(32f to 94f, 44.1f to 86f, 71.1f to 76f, 68f to 68f, 39.0f to 58f, 47f to 54f, 62f to 46f, 55f to 35f, 51.2f to 26f, 71.1f to 15f),
    5 to listOf(72f to 92f, 68f to 84f, 54.2f to 75f, 36.6f to 67f, 71.3f to 57f, 55.2f to 53f, 70.0f to 47f, 45f to 36f, 36.5f to 27f, 48f to 17f),
    6 to listOf(51.8f to 92f, 42f to 84f, 60f to 73f, 70f to 63f, 68.3f to 56f, 58f to 49f, 68.3f to 42f, 72f to 33f, 68.3f to 25f, 80.4f to 15f),
    7 to listOf(42.9f to 93f, 40.2f to 85f, 76.1f to 77f, 84.5f to 70f, 63.6f to 64f, 50f to 53f, 73.4f to 45f, 65f to 36f, 65.1f to 26f, 62f to 14f),
    8 to listOf(32f to 91f, 40f to 80f, 55f to 73f, 70f to 66f, 68.9f to 56f, 50f to 50f, 50f to 43f, 75.7f to 36f, 45f to 29f, 65f to 17f),
    9 to listOf(80f to 93f, 65.0f to 86f, 52.8f to 80f, 36.1f to 72f, 53.4f to 67f, 78.9f to 58f, 54.3f to 52f, 74.9f to 44f, 62.0f to 24.0f, 64.0f to 16.0f)
)

/** The colour of each artwork's bottom edge, which carries on under the panel where the picture ends. */
internal val LevelArtBottomColors: Map<Int, Long> = mapOf(
    1 to 0xFF7E7033L,
    2 to 0xFF677269L,
    3 to 0xFF64642CL,
    4 to 0xFF82764DL,
    5 to 0xFF60492BL,
    6 to 0xFF7A653AL,
    7 to 0xFF7F6F3BL,
    8 to 0xFF58562CL,
    9 to 0xFF605826L
)
