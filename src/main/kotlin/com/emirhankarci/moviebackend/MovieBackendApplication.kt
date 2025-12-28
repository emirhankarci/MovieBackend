package com.emirhankarci.moviebackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MovieBackendApplication

fun main(args: Array<String>) {
    runApplication<MovieBackendApplication>(*args)
}
