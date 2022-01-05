# Improve Maintainability

* Status: accepted
* Deciders: Sergio Sacristán
* Date: 2021-12-22

## Context and Problem Statement

In order to evolve DocGen service and migrate here the LevaDoc feature, initially implemented
in the SharedLib, we need to improve LevaDoc architecture to make it more maintainable.

## Decision Drivers

* Maintainability: speed up the development with better modularization of the code
* Testability.
* Extensibility.
* Performance (of course we should take care of Performance, but as the API will be executed from a batch, 
we don't care if the response takes 2 seconds more or less)

## Considered Options

* jooby with Dagger and Groovy: poor documentation and examples
* jooby with SpringFramework and Groovy: poor documentation and examples
* SpringFramework and Groovy

## Decision Outcome

Chosen option: "SpringFramework", because:
- It has the best integration with more frameworks, means also better extensibility
- There's a lot of documentation and examples. Easy to solve problems
- There's a bigger community of users: easy to involve new developers

### Negative Consequences 

* We should do a big refactor

