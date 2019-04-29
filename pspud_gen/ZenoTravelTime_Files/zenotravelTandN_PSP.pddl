(define (domain zeno-travel)
(:requirements :durative-actions :typing :fluents)
(:types aircraft person city - object)
(:predicates (at ?x - (either person aircraft) ?c - city)
             (in ?p - person ?a - aircraft))
(:functions (fuel ?a - aircraft)
            (distance ?c1 - city ?c2 - city)
            (slow-speed ?a - aircraft)
            (fast-speed ?a - aircraft)
            (slow-burn ?a - aircraft)
            (fast-burn ?a - aircraft)
            (capacity ?a - aircraft)
            (refuel-rate ?a - aircraft)
            (total-fuel-used)
            (boarding-time ?c - city)
            (debarking-time ?c - city)
            
            (labor-cost ?c - city)
            (fuel-cost ?c - city)
            (maintainance-cost ?a - aircraft)
            (airport-aircraft-fee ?a - aircraft ?c - city)
            (airport-fee ?c - city)
            )


(:durative-action board
 :parameters (?p - person ?a - aircraft ?c - city)
 :duration (= ?duration (boarding-time ?c))
 :cost (+ (* (boarding-time ?c) (labor-cost ?c)) (airport-fee ?c))
 :condition (and (at start (at ?p ?c))
                 (over all (at ?a ?c)))
 :effect (and (at start (not (at ?p ?c)))
              (at end (in ?p ?a))))


(:durative-action debark
 :parameters (?p - person ?a - aircraft ?c - city)
 :duration (= ?duration (debarking-time ?c))
 :cost (+ (* (debarking-time ?c) (labor-cost ?c)) (airport-fee ?c))
 :condition (and (at start (in ?p ?a))
                 (over all (at ?a ?c)))
 :effect (and (at start (not (in ?p ?a)))
              (at end (at ?p ?c))))


(:durative-action fly 
 :parameters (?a - aircraft ?c1 ?c2 - city)
 :duration (= ?duration (/ (distance ?c1 ?c2) (slow-speed ?a)))
 :cost (+ (airport-aircraft-fee ?a ?c1) (+ (airport-aircraft-fee ?a ?c2)
 			(* (distance ?c1 ?c2) (maintainance-cost ?a))))
 :condition (and (at start (at ?a ?c1))
                 (at start (>= (fuel ?a) 
                         (* (distance ?c1 ?c2) (slow-burn ?a)))))
 :effect (and (at start (not (at ?a ?c1)))
              (at end (at ?a ?c2))
              (at end (increase total-fuel-used
                         (* (distance ?c1 ?c2) (slow-burn ?a))))
              (at end (decrease (fuel ?a) 
                         (* (distance ?c1 ?c2) (slow-burn ?a)))))) 

                                  
(:durative-action zoom
 :parameters (?a - aircraft ?c1 ?c2 - city)
 :duration (= ?duration (/ (distance ?c1 ?c2) (fast-speed ?a)))
 :cost (+ (airport-aircraft-fee ?a ?c1) (+ (airport-aircraft-fee ?a ?c2)
 			(* (distance ?c1 ?c2) (maintainance-cost ?a))))
 :condition (and (at start (at ?a ?c1))
                 (at start (>= (fuel ?a) 
                         (* (distance ?c1 ?c2) (fast-burn ?a)))))
 :effect (and (at start (not (at ?a ?c1)))
              (at end (at ?a ?c2))
              (at end (increase total-fuel-used
                         (* (distance ?c1 ?c2) (fast-burn ?a))))
              (at end (decrease (fuel ?a) 
                         (* (distance ?c1 ?c2) (fast-burn ?a)))))) 


(:durative-action refuel
 :parameters (?a - aircraft ?c - city)
 :duration (= ?duration (/ (- (capacity ?a) (fuel ?a)) (refuel-rate ?a)))
 :cost (* (fuel-cost ?c) (- (capacity ?a) (fuel ?a)))
 :condition (and (at start (> (capacity ?a) (fuel ?a)))
                 (over all (at ?a ?c)))
 :effect (at end (assign (fuel ?a) (capacity ?a))))
)
