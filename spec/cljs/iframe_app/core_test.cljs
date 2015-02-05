(ns core-test
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
            [iframe-app.core :refer [main app]]))

(enable-console-print!)
(set! (.-warn js/console) (fn [t] nil))
(def p println)

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


(defn element-click-fn [element-getter]
  (fn [arg]
    (click (element-getter arg))))

(def behaviour->action
  {:select-master-field {:element-getter (element-click-fn get-master-field-element)
                         :transform      (fn [state new-value]
                                           (assoc state :master-field new-value))}
   :select-field-value  {:element-getter (element-click-fn get-field-value-element)
                         :transform      (fn [state arg]
                                           (assoc state :field-value arg))}
   :select-slave-field  {:element-getter (element-click-fn get-slave-field-element)
                         :transform      (fn [fields arg]
                                           (update-in fields [:slave-fields] conj arg))}})



(defn behave [[behaviour-name arg]]
  (when-let [action (:element-getter (behaviour->action behaviour-name))]
    (action arg)))

(defn selection-state-matches?
  "ensure that the expected state matches the app-state atom and the DOM"
  [state]

  (= (:selections @app-state)
     state))

(defn state-matches-dom? [selections]
  (let [master-field (:master-field selections)
        master-field-el (get-master-field-element master-field)
        actual-id (string->int (dommy/attr master-field-el :data-id))]
    (= actual-id (:id master-field))))

(defn get-states-for-behaviors [behaviours]
  (reductions (fn [state {:keys [transform-fn value]}]
                (transform-fn state value))
              {:master-field nil
               :field-value  nil
               :slave-fields #{}}
              (map (fn [[behaviour-name value :as behaviour]]
                     {:transform-fn (:transform (behaviour->action behaviour-name))
                      :value        value})
                   behaviours)))

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

(defn dummy-behaviour [ticket-fields]
  (let [master-field (rand-nth ticket-fields)
        field-value (rand-nth (:possible-values master-field))
        slave-field (rand-nth (fields-without-field ticket-fields
                                                    master-field))]
    [[:select-master-field master-field],
     [:select-field-value field-value],
     [:select-slave-field slave-field]]))


(deftest ^:async can-make-conditions
  (let [ticket-fields (first (gen/sample ticket-fields-gen 1))
        behaviour (dummy-behaviour ticket-fields)
        states (get-states-for-behaviors behaviour)
        behaviours-with-state-afterwards (map vector behaviour (rest states))]

    (setup-app ticket-fields)
    (go
      (doseq [[behaviour after-state] behaviours-with-state-afterwards]
        (behave behaviour)
        (wait-a-bit)


        (is (selection-state-matches? after-state))
        (is (state-matches-dom? after-state)))
      (dommy/set-html! (.-body js/document) "")
      (done))))




(set! (.-onload js/window)
      (fn []
        (t/run-all-tests)
        (js/setTimeout
          (fn []
            (.callPhantom js/window "exit"))
          3000)))
