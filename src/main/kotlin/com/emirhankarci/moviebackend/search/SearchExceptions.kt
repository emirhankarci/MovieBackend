package com.emirhankarci.moviebackend.search

class InvalidQueryException(message: String) : RuntimeException(message)

class InvalidGenreException(message: String) : RuntimeException(message)

class InvalidRatingRangeException(message: String) : RuntimeException(message)

class ExternalServiceException(message: String) : RuntimeException(message)
