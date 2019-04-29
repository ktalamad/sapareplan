;; seed: 42
(define (domain zeno-travel)
(:requirements :typing )
 (:types
 aircraft flevel person city - object
 )
(:predicates
 (at ?a - (either person aircraft) ?b - city)
 (in ?a - person ?b - aircraft)
 (fuel-level ?a - aircraft ?b - flevel)
 (next ?a - flevel ?b - flevel)
 (is-true))
 
(:action board
:parameters (?p - person ?a - aircraft ?c - city)
:cost (board-cost ?p ?a)
:precondition (and 
(at ?p ?c)
(at ?a ?c)
)
:effect (and
(not (at ?p ?c))
(in ?p ?a)
)
)
(:action debark
:parameters (?p - person ?a - aircraft ?c - city)
:cost (debark-cost ?p ?a)
:precondition (and 
(in ?p ?a)
(at ?a ?c)
)
:effect (and
(not (in ?p ?a))
(at ?p ?c)
)
)
(:action fly
:parameters (?a - aircraft ?c1 - city ?c2 - city ?l1 - flevel ?l2 - flevel)
:cost (fly-cost ?a ?c1 ?c2)
:precondition (and 
(at ?a ?c1)
(fuel-level ?a ?l1)
(next ?l2 ?l1)
)
:effect (and
(not (at ?a ?c1))
(at ?a ?c2)
(not (fuel-level ?a ?l1))
(fuel-level ?a ?l2)
)
)
(:action zoom
:parameters (?a - aircraft ?c1 - city ?c2 - city ?l1 - flevel ?l2 - flevel ?l3 - flevel)
:cost (zoom-cost ?a ?c1 ?c2)
:precondition (and 
(at ?a ?c1)
(fuel-level ?a ?l1)
(next ?l2 ?l1)
(next ?l3 ?l2)
)
:effect (and
(not (at ?a ?c1))
(at ?a ?c2)
(not (fuel-level ?a ?l1))
(fuel-level ?a ?l3)
)
)
(:action refuel
:parameters (?a - aircraft ?c - city ?l - flevel ?l1 - flevel)
:cost (refuel-cost ?a ?c)
:precondition (and 
(fuel-level ?a ?l)
(next ?l ?l1)
(at ?a ?c)
)
:effect (and
(fuel-level ?a ?l1)
(not (fuel-level ?a ?l))
)
))
