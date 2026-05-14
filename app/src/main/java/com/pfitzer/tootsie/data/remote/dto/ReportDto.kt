package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportDto(@SerialName("id") val id: String)
