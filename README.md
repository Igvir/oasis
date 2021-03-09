[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Build Status](https://github.com/isuru89/oasis/workflows/Oasis-ci-test/badge.svg)
[![Known Vulnerabilities](https://snyk.io/test/github/isuru89/oasis/badge.svg)](https://snyk.io/test/github/isuru89/oasis)
[![coverage](https://codecov.io/gh/isuru89/oasis/branch/master/graph/badge.svg)](https://codecov.io/gh/isuru89/oasis)


_This project is still under development_

# OASIS
Open-source Gamification framework based on Redis.

Oasis, is an event-driven gamification framework having ability to define the game rules for events
coming from your applications. This is inspired from Stackoverflow badge system, and extended into
supporting many other game elements, such as, points, badges, leaderboards,
milestones, challenges, and ratings.

## Features:
* Different types of customizable [gamification elements](#game-elements).
* Near real-time status updates
* Embeddable game engine
* Users can play in teams
* Modular design, so its easier to extend to the specific needs
* Out of order event support

## Contents
* [Architecture of Oasis](#architecture-of-oasis)
* [Running Modes](#running-modes)
    * [Embedding Engine](#embedding-engine)
    * [Engine as a service](#engine-as-a-service)
* [Concepts](#concepts)
    * [Participants](#participants)
    * [Game Elements](#game-elements)
* [Why Oasis?](#why-oasis)
* [Contributing](#contributing)
* [Roadmap](#roadmap)
* [Kudos!](#kudos)
* [License](#license)

## Architecture of Oasis
![Oasis Architecture](/docs/images/oasis-arch.png?raw=true "Oasis Architecture")

* **Events API**: The api can be exposed to public networks where event-sources can publish events to the framework.
  This authorize the event sources and game model before accepting any events.
* **Stats API**: This api is for manipulating [game model](#gamification-model) (or in other word admin-related operations) and
  querying statistics. Through this API, model entities can be created/updated/removed and those
  changes will be reflected in engine too. This API is only for internal purpose.
* **Engine**: This is the heart of the framework which create rewards by evaluating received events based on the defined
  rules in the system.

As of initial version, Redis will act as the database for engine operations considering the performance.


## Running Modes

### Engine as a service

This is a full deployment with all the components as shown in [Architecture](#architecture-of-oasis).
This provides out-of-the-box components which can be used by your applications.

For testing purpose, a docker compose setup has been provided to up and running locally.

Kubernetes and AWS solution are still pending.

### Embedding Engine

One of the important thing about Oasis is that you can embed the game engine
inside your application. In this way, you have to pump all your events and getting
data/statistics out of Redis.

Its very simple.

```java
public static void main(String[] args) {
    // load the engine configuration (we use typesafe configs here)
    var oasisConfigs = OasisConfigs.defaultConfigs();
    
    // initialize the engine database: redis
    var dbPool = RedisDb.create(oasisConfigs);
    dbPool.init();

    // required for storing individual events for some game element rules.
    // for now you can use the same Redis instance to store events.
    var eventStore = new RedisEventLoader(dbPool, oasisConfigs);

    // add elements as you desired for your application
    EngineContext context = EngineContext.builder()
            .withConfigs(oasisConfigs)
            .withDb(dbPool)
            .withEventStore(eventStore)
            .installModule(RatingsModuleFactory.class)
            .installModule(PointsModuleFactory.class)
            .installModule(MilestonesModuleFactory.class)
            .installModule(ChallengesModuleFactory.class)
            .installModule(BadgesModuleFactory.class)
            .build();
    
    var engine = new OasisEngine(context);
    engine.start(); 
    
    engine.submit(event) // you can submit your event(s) now.
}

```

**Note:** Once you start the engine, it will keep running until the application goes down.

Check the methods of `OasisEngine` class how you can submit your events.


## Concepts

## Participants
It is very important to understand supporting model before going down to the details.
There are four major participants in the system.

### Entities
#### Game:
A game is a boundary of all game elements. Manipulation of games can only be done by an admin thorugh Stats API.


#### Player:
A player is an entity who associate with events and get rewarded from elements.
Players can register to the system only by Admins and they will be uniquely identified by email address.
Also Players needs to associate with a game through a team. That means every player must be a member of team.


If an event cannot be correlate to a existing player in the system, that event will be discarded and will
not be processed by engine. So, it is important to register all applicable users to the system before such
event is arrived. Still you can add players while the game is running.

#### Team:
A team is formed by grouping several players together within a game.
A team name must be unique across all games (i.e. system).

Purpose of a team is to associate players to a game, plus,
provide leaderboards based on awards generated from game rules.
Sometimes it is not fair someone to compete with whole players of a game.


#### Event Source:
An external/internal party which generated actual events which can be evaluated by engine.
These event sources must be first registered with the system and download the private key.
Only an Admin can register event sources and map those to games.
And after that, private key must be used to sign the payload of events being dispatched.

Once received those events to Events API, it will check for the integrity of messages by comparing
the signature and will allow further processing.


### Rules of Relationships
Rules of relationship between the above entities can be described as shown below.
* **Rule-1**: There can be many Games running at the same time in a [single deployment](#engine-as-a-service).
* **Rule-2**: A Player can play in many Games at once.
* **Rule-3**: A Player can play a Game _only_ by association with one Team.
  Assigning to multiple Teams within a single Game is not allowed.
* **Rule-4**: A Player may change his/her Team, or leave the Game completely.
* **Rule-5**: An Event-Source and Game has a many-to-many relationship,
  so that a Game can have multiple sources while a source may push events to multiple games.


## Game Elements

All these game elements, except Attributes and Leaderboards, can be defined in yaml files and register to the engine.
Attributes must be defined when a game is created through admin-api.

### Attributes
Each game should define set of awarding attributes to rank some of game elements.
For e.g. attributes equivalent in Stackoverflow are gold, silver and bronze.

### Points

_See a sample point specification_ [here](engine/src/test/resources/rules/points-basic.yml)

One of the core element type in Oasis. The points indicate a measurement about a user against an event.
Users can accumulate points over the time as rules defined by the admin or curator.
Sometimes, points can be negative, hence called penalties.

### Badges

_See a sample badge specification_ [here](engine/src/test/resources/rules/badges-basic.yml)

A badge is a collectible achievement by a user based on correlating one or several
events. Every badge can associate with an attribute.

There are several kinds of badges supported by Oasis.

* An event has occurred for the first time (eg: [Stackoverflow Altruist badge](https://stackoverflow.com/help/badges/222/altruist) )
* An event satisfies a certain criteria (eg: [Stackoverflow Popular Question](https://stackoverflow.com/help/badges/26/popular-question) )
    * For different thresholds can award different sub-badges
    * (eg: [Stackoverflow Famous question](https://stackoverflow.com/help/badges/28/famous-question) )
* Streaks:
    * Satisfies a condition for N consecutive times. (eg: [Stackoverflow Enthusiast](https://stackoverflow.com/help/badges/71/enthusiast) )
    * Satisfies a condition for N consecutive times within T time-unit.
    * Satisfies a condition for N times within T time-unit. (eg: [Stackoverflow Curious badge](https://stackoverflow.com/help/badges/4127/curious) )
* Earn K points within a single time-unit (daily/weekly/monthly)
    * Eg: [Stackoverflow Mortarboard badge](https://stackoverflow.com/help/badges/144/mortarboard)
* Daily accumulation of an event field is higher than a threshold (T) for,
    * N consecutive days. (eg: Earn 50 daily reputation for 10 consecutive days)
    * N separate days (eg: Earn 200 daily reputation for 50 consecutive days)
* Manually
    * Curators and admin can award badges to players based on non-measurable activities.

### Milestones

_See a sample milestone specification_ [here](engine/src/test/resources/rules/milestone-basic.yml)

Milestone can be created to accumulate points over the lifecycle of a game.
It indicates the progress gained by a user. Milestones are always being based on the points
scored by a user.

Milestones can be used to give a *rank* (called Levels in Oasis) to a user based on the current accumulated value.
Eg: In Stackoverflow, the total Reputation earned can be defined as a milestone definition and levels
can be defined in such a way,
* Scoring 10k reputation - Level 1
* Scoring 50k reputation - Level 2
* Scoring 100k reputation - Level 3
* Scoring 500k reputation - Level 4
* Scoring 1M reputation - Level 5


### Challenges

_See a sample challenge specification_ [here](engine/src/test/resources/rules/challenges-basic.yml)

Challenge can be created by a curator at any time of game lifecycle
to motivate a user towards a very short term goal. Challenge must have a start time
and end time.
A challenge can be scoped to a single user, team, or a game. It also can be defined to
have single winner or multiple winners. Winners are being awarded First-come basis.

A challenge can be completed in two ways, whichever comes first.
* Number of all winners found.
* Time has expired

### Ratings

_See a sample rating specification_ [here](engine/src/test/resources/rules/ratings-basic.yml)

Ratings indicate the current state of a user at a particular time. Based on the events, user's
status will be calculated, and from that status, some amount of net points will be awarded.
_A user can only be in one state at a time_. A difference between Milestone and Rating would
be ratings can fluctuate to any direction, while milestones can only grow.


### Leaderboards
Oasis provides leaderboards based on points scored by users. 
Leaderboards are scoped to game and team levels.

Each leaderboard can be viewed scoped on the time slots. Supported time slots are; daily, weekly,
monthly, quarterly and annually.

**_Note_**: Oasis will not support global leaderboards where players can be compared across games.

## Why Oasis?

Ultimate objective of the Oasis is to increase the user engagement in applications
through a gamified environment.
Oasis might help your applications/community to increase the productivity
and could help in creating a better and enjoyable experience.

Following gamifiable environments have been identified.
- SDLC: whole software development lifecycle (coding, bug fixing, deployments) using the
  application stack (Code Quality, CI/CD, ALM tools)
- Support Systems: IT helpdesk systems
- Customer Loyalty frameworks
- Q/A sites: Stackoverflow, Reddit like sites
- Social Networking

## Roadmap
* Character driven game playing
* Narrative Play
* Cloud friendly

## Contributing
TBW

## Kudos!

This project could not have existed thanks to these awesome open-source projects.

* [Redis](https://redis.io/)
* [Akka](https://akka.io/)
* [Vert.x](https://vertx.io/)
* [Spring-boot](https://spring.io/projects/spring-boot)
* [MVEL](https://github.com/mvel/mvel)

## License

Apache License - version 2.0
