(ns iframe-app.journeys-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var
                               done are)]
                   [iframe-app.macros :refer [wait-a-bit]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test :as t]
            [om.core :as om :include-macros true]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >!]]
            [iframe-app.utils :refer [fire! click string->int]]
            [iframe-app.generators :refer [ticket-field-gen ticket-fields-gen]]
            [cljs.test.check.generators :as gen]
            [iframe-app.condition-selector :refer [fields-without-field]]
            [iframe-app.journeys :as j :refer [possible-selection-states
                                               generate-user-journey]]
            [iframe-app.core :refer [main app]]))



(defn generate-ticket-fields []
  {:post [(not-empty %)]}
  (let [ticket-fields (first (gen/sample ticket-fields-gen 1))]
    ticket-fields))

;(deftest possible-selection-states-includes-empty-state
;  (let [states (possible-selection-states (generate-ticket-fields))]
;    (is (> (count states) 1))
;    (is (contains? states {:master-field nil
;                           :field-value nil}))))

(deftest next-actions-for-state-works
  (let [ticket-fields (generate-ticket-fields)
        state (first (possible-selection-states ticket-fields))
        actions (j/next-actions-for-state state ticket-fields)]
    (is true)))

(deftest generate-journeys
  (let [ticket-fields (generate-ticket-fields)
        journey (j/generate-user-journey ticket-fields)]
    ;(doseq [bit journey]
    ;  (println)
    ;  (println bit)
    ;  (println)
    (is (not-empty journey)))))

;(enable-console-print!)
;(set! (.-warn js/console) (fn [t] nil))
;(def p println)
;
;
;
;(set! (.-onload js/window)
;      (fn []
;        (t/run-all-tests)
;        (js/setTimeout
;          (fn []
;            (.callPhantom js/window "exit"))
;          3000)))