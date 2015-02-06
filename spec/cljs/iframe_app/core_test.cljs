(ns iframe-app.core-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var
                               done are)]
                   [iframe-app.macros :refer [wait-a-bit]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test :as t]
            [om.core :as om :include-macros true]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >!]]
            [iframe-app.utils :refer [fire! click string->int field-values-from-fields]]
            [iframe-app.generators :refer [ticket-field-gen ticket-fields-gen]]
            [cljs.test.check.generators :as gen]
            [iframe-app.condition-selector :refer [fields-without-field]]
            [iframe-app.journeys :as j :refer [possible-selection-states
                                               transform
                                               generate-user-journey]]
            [iframe-app.core :refer [main app]]))



(enable-console-print!)
(set! (.-warn js/console) (fn [t] nil))
(def p println)

(defn np [m]
  (println "===============")
  (p m)
  (println "==============="))

(defonce app-state (atom {:conditions #{}}))

; TODO: use test.check to generate behaviour
; TODO: make finders that wait til they find what they're looking for (like Capybara)


(defn get-field-by [field-type by field]
  (sel1 (str "a." field-type "[data-" by "='" ((keyword by) field) "']")))

(def get-master-field-element
  (partial get-field-by "master-field" "id"))

(def get-field-value-element
  (partial get-field-by "field-value" "value"))

(def get-slave-field-element
  (partial get-field-by "slave-field" "id"))

; for master field
; identifier: id


(defmulti perform :action-type)

(defmethod perform :select-master-field
  [{:keys [value]}]
  (click (get-master-field-element value)))

(defmethod perform :select-field-value
  [{:keys [value]}]
  (click (get-field-value-element value)))

(defmethod perform :toggle-slave-field
  [{:keys [value]}]
  (click (get-slave-field-element value)))

;(def behaviour->action
;  {   :select-slave-field
;                         :transform      (fn [fields arg]
;                                           (update-in fields [:slave-fields] conj arg))}})


(defn state-matches-dom? [{:keys [selections]}]
  (let [master-field (:master-field selections)
        master-field-el (get-master-field-element master-field)
        actual-id (string->int (dommy/attr master-field-el :data-id))]
    (= actual-id (:id master-field))))

(defn get-states-for-actions [actions]
  (reductions (fn [state action]
                (transform action state))

              j/starting-state
              actions))

(defn setup-app [ticket-fields]
  (.appendChild (.-body js/document)
                (let [element (.createElement js/document "div")
                      _ (set! (.-id element) "app")]
                  element))
  (om/root app
           app-state
           {:target (. js/document (getElementById "app"))
            :shared {:pick-channel  (chan)
                     :ticket-fields ticket-fields}}))


(deftest ^:async can-make-conditions
  (let [ticket-fields (first (gen/sample ticket-fields-gen 1))
        actions (generate-user-journey ticket-fields)
        states (get-states-for-actions actions)
        actions-with-after-state (map vector actions (rest states))]
    (doseq [[action state] (take 5 actions-with-after-state)]
      (np action)
      (np state))

    (setup-app ticket-fields)
    (go
      (doseq [[action after-state] actions-with-after-state]
        (perform action)
        (wait-a-bit)

        (is (= after-state @app-state) )
        ;(is (state-matches-dom? after-state))
        )
      (dommy/set-html! (.-body js/document) "")
      (swap! iframe-app.generators/ints-used-so-far (fn [_] #{}))
      (done)
      (.callPhantom js/window "exit"))))




(set! (.-onload js/window)
      (fn []
        ;(t/run-all-tests)
        (run-tests 'iframe-app.core-test)
        ;(.callPhantom js/window "exit")
        (js/setTimeout
          (fn []
            (.callPhantom js/window "exit"))
          3000)
        ))




