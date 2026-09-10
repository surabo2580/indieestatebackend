package com.indieestate.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.indieestate.backend"])
class IndieestateBackendApplication

fun main(args: Array<String>) {
    runApplication<IndieestateBackendApplication>(*args)
}
