package com.sualtikasifi.cizimhafiza.presentation.levelmap

/**
 * Where the ten stops of each world sit on that world's artwork (bg_world_N), as percentages of the picture's
 * width and height, level 1 first. The path is painted into the picture, so the stops are placed by hand.
 */
internal val LevelNodePositions: Map<Int, List<Pair<Float, Float>>> = mapOf(
    1 to listOf(42f to 93f, 36f to 83f, 50f to 79f, 66f to 72f, 66f to 60f, 72f to 48f, 68f to 38f, 56f to 28f, 62f to 20f, 68f to 14f),
    2 to listOf(40f to 92f, 52f to 82f, 55f to 72f, 40f to 62f, 33f to 54f, 52f to 46f, 70f to 38f, 70f to 30f, 58f to 22f, 72f to 16f),
    3 to listOf(45f to 95f, 60f to 86f, 62f to 77f, 45f to 70f, 35f to 62f, 55f to 52f, 45f to 43f, 50f to 33f, 60f to 26f, 65f to 18f),
    4 to listOf(32f to 94f, 45f to 86f, 60f to 76f, 68f to 68f, 45f to 58f, 47f to 54f, 62f to 46f, 55f to 35f, 62f to 26f, 75f to 15f),
    5 to listOf(72f to 92f, 68f to 84f, 60f to 75f, 45f to 67f, 60f to 57f, 68f to 53f, 60f to 47f, 45f to 36f, 50f to 27f, 48f to 17f),
    6 to listOf(50f to 92f, 42f to 84f, 60f to 73f, 70f to 63f, 65f to 56f, 58f to 49f, 62f to 42f, 72f to 33f, 65f to 25f, 68f to 15f),
    7 to listOf(55f to 93f, 45f to 85f, 62f to 77f, 72f to 70f, 78f to 64f, 50f to 53f, 62f to 45f, 65f to 36f, 55f to 26f, 62f to 14f),
    8 to listOf(32f to 91f, 40f to 80f, 55f to 73f, 70f to 66f, 62f to 56f, 50f to 50f, 50f to 43f, 63f to 36f, 45f to 29f, 65f to 17f),
    9 to listOf(80f to 93f, 68f to 86f, 55f to 80f, 45f to 72f, 60f to 67f, 70f to 58f, 62f to 52f, 60f to 44f, 48f to 35f, 65f to 22f)
)
