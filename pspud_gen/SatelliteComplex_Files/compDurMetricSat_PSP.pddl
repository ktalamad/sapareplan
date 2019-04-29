
(define (domain satellite)
 (:requirements :strips :equality :typing :fluents :durative-actions)
 (:types satellite direction instrument mode)
 (:predicates 
           (on_board ?i - instrument ?s - satellite)
	       (supports ?i - instrument ?m - mode)
	       (pointing ?s - satellite ?d - direction)
	       (power_avail ?s - satellite)
	       (power_on ?i - instrument)
	       (calibrated ?i - instrument)
	       (have_image ?d - direction ?m - mode)
	       (calibration_target ?i - instrument ?d - direction))
 
 

  (:functions 
  		(slew_time ?a ?b - direction)
		(calibration_time ?a - instrument ?d - direction)
		(data_capacity ?s - satellite)
		(data ?d - direction ?m - mode)
		(data-stored)
		
		;; Additional functions for cost (PSP) purpose
		(slew_energy_rate ?s - satellite)
		(switch_on_energy ?s - satellite ?i - instrument)
		(switch_off_energy ?s - satellite ?i - instrument)
		(calibration_energy_rate ?s - satellite ?i - instrument)
		(data_process_energy_rate ?s - satellite ?i - instrument)
   )

 

  (:durative-action turn_to
   :parameters (?s - satellite ?d_new - direction ?d_prev - direction)
   :duration (= ?duration (slew_time ?d_prev ?d_new))
   :cost (* (slew_time ?d_prev ?d_new) (slew_energy_rate ?s))
   :condition (and (at start (pointing ?s ?d_prev))
                   (over all (not (= ?d_new ?d_prev)))
              )
   :effect (and  (at end (pointing ?s ?d_new))
                 (at start (not (pointing ?s ?d_prev)))
           )
  )

 
  (:durative-action switch_on
   :parameters (?i - instrument ?s - satellite)
   :duration (= ?duration 2)
   :cost (switch_on_energy ?s ?i)
   :condition (and (over all (on_board ?i ?s)) 
                      (at start (power_avail ?s)))
   :effect (and (at end (power_on ?i))
                (at start (not (calibrated ?i)))
                (at start (not (power_avail ?s)))
           )
          
  )

 
  (:durative-action switch_off
   :parameters (?i - instrument ?s - satellite)
   :duration (= ?duration 1)
   :cost (switch_off_energy ?s ?i)
   :condition (and (over all (on_board ?i ?s))
                      (at start (power_on ?i)) 
                  )
   :effect (and (at start (not (power_on ?i)))
                (at end (power_avail ?s))
           )
  )

  (:durative-action calibrate
   :parameters (?s - satellite ?i - instrument ?d - direction)
   :duration (= ?duration (calibration_time ?i ?d))
   :cost (* (calibration_time ?i ?d) (calibration_energy_rate ?s ?i))
   :condition (and (over all (on_board ?i ?s))
		      (over all (calibration_target ?i ?d))
                      (at start (pointing ?s ?d))
                      (over all (power_on ?i))
                      (at end (power_on ?i))
                  )
   :effect (at end (calibrated ?i)) 
  )


  (:durative-action take_image
   :parameters (?s - satellite ?d - direction ?i - instrument ?m - mode)
   :duration (= ?duration 7)
   :cost (* (data ?d ?m) (data_process_energy_rate ?s ?i)) 
   :condition (and (over all (calibrated ?i))
					(over all (on_board ?i ?s))
					(over all (supports ?i ?m))
					(over all (power_on ?i))
					(over all (pointing ?s ?d))
					(at end (power_on ?i))
					(at start (>= (data_capacity ?s) (data ?d ?m)))
               )
   :effect (and (at start (decrease (data_capacity ?s) (data ?d ?m)))
		(at end (have_image ?d ?m))
		(at end (increase (data-stored) (data ?d ?m)))
		)
  )
)