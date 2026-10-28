package com.sualtikasifi.cizimhafiza.domain.model

/** Why a player was reported (playerReports/{reportedUid}__{reporterUid}.reason; the names are what firestore.rules accepts). */
enum class PlayerReportReason { NAME, BEHAVIOUR, CHEATING, OTHER }
