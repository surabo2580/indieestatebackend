package com.indieestate.backend.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal val chatJson: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
