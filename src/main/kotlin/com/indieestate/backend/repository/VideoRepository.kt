package com.indieestate.backend.repository

import com.indieestate.backend.entity.VideoEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VideoRepository : JpaRepository<VideoEntity, UUID>
