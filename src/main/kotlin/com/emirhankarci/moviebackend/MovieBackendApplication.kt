package com.emirhankarci.moviebackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MovieBackendApplication

fun main(args: Array<String>) {
    runApplication<MovieBackendApplication>(*args)
}
