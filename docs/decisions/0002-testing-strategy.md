# Autoamated testing strategy

* Status: accepted
* Deciders: Sergio Sacristán
* Date: 2021-12-22

## Context and Problem Statement

When unit testing a service, the standard unit is usually the service class, simple as that. The test will mock out the layer underneath  in this case the DAO/DAL layer and verify the interactions on it. Exact same thing for the DAO layer  mocking out the interactions with the database (HibernateTemplate in this example) and verifying the interactions with that.

This is a valid approach, but it leads to brittle tests  adding or removing a layer almost always means rewriting the tests entirely. This happens because the tests rely on the exact structure of the layers, and a change to that means a change to the tests.
To avoid this kind of inflexibility, we can grow the scope of the unit test by changing the definition of the unit  we can look at a persistent operation as a unit, from the Service Layer through the DAO and all the way day to the raw persistence  whatever that is. Now, the unit test will consume the API of the Service Layer and will have the raw persistence mocked out  in this case, the "templates.repository"

## Decision Drivers 

* Optimize testing effort
* Improve test quality

## Decision Outcome

https://github.com/portainer/portainer
https://localhost:9443/
admin 12345678

### Positive Consequences <!-- optional -->

* {e.g., improvement of quality attribute satisfaction, follow-up decisions required, …}
* …

### Negative Consequences <!-- optional -->

* {e.g., compromising quality attribute, follow-up decisions required, …}
* …

## Pros and Cons of the Options <!-- optional -->

### {option 1}

{example | description | pointer to more information | …} <!-- optional -->

* Good, because {argument a}
* Good, because {argument b}
* Bad, because {argument c}
* … <!-- numbers of pros and cons can vary -->

### {option 2}

{example | description | pointer to more information | …} <!-- optional -->

* Good, because {argument a}
* Good, because {argument b}
* Bad, because {argument c}
* … <!-- numbers of pros and cons can vary -->

### {option 3}

{example | description | pointer to more information | …} <!-- optional -->

* Good, because {argument a}
* Good, because {argument b}
* Bad, because {argument c}
* … <!-- numbers of pros and cons can vary -->

## Links <!-- optional -->

* {Link type} {Link to ADR} <!-- example: Refined by [ADR-0005](0005-example.md) -->
* … <!-- numbers of links can vary -->

<!-- markdownlint-disable-file MD013 -->





